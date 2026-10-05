package com.saintchigos.studyhub.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintchigos.studyhub.data.DailyAlarm
import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.reminder.DailyAlarms
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the focus timer is doing right now. */
sealed interface FocusPhase {
    data object Idle : FocusPhase
    data class Running(val plannedMinutes: Int, val label: String) : FocusPhase
    data class Paused(val plannedMinutes: Int, val label: String, val remainingSeconds: Int) :
        FocusPhase

    data class Finished(val actualMinutes: Int, val label: String) : FocusPhase
}

/**
 * Focus timer, study log and wake-up alarms.
 *
 * The countdown is derived from wall-clock time rather than counted down tick by
 * tick, so it stays accurate if Android delays the coroutine or the phone sleeps.
 */
class FocusViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = StudyHubDatabase.get(app).dao()
    private val context: Context = app.applicationContext

    private val _phase = MutableStateFlow<FocusPhase>(FocusPhase.Idle)
    val phase: StateFlow<FocusPhase> = _phase.asStateFlow()

    /** Wall-clock millis when the current run ends, or null when not running. */
    private val _endsAt = MutableStateFlow(0L)
    private val _remainingSeconds = MutableStateFlow(25 * 60)

    /**
 * Ticks once a second only while a run is active.
 *
 * Seeded from the default preset so the idle dial shows the chosen length rather
 * than 00:00, which read as "the timer is finished" before anything was started.
 */
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _elapsedInRun = MutableStateFlow(0L)
    private var tickJob: Job? = null

    /** Wall-clock start of the current run, kept out of the StateFlow since nothing observes it. */
    private var startedAt: Long = 0L

    /**
     * Wall-clock millis already spent paused.
     *
     * Pausing has to be subtracted before logging, otherwise walking away for an
     * hour with the timer paused would be logged as an hour of study.
     */
    private var pausedMillis = 0L

    /** Set when pause() runs, cleared on resume, so the paused stretch is measurable. */
    private var pausedAt = 0L

    private val _selectedMinutes = MutableStateFlow(25)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    /** Mirrors the chosen length so the dial has something to show before a run. */
    private val _idleSeconds = MutableStateFlow(25 * 60)

    private val _selectedLabel = MutableStateFlow("Deep work")
    val selectedLabel: StateFlow<String> = _selectedLabel.asStateFlow()

    val recentSessions = dao.observeRecentFocusSessions(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals = dao.observeFocusTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalMinutes = dao.observeFocusMinutesTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    

    val alarms = dao.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total sessions on file, so "Clear log" can say the true count. */
    val sessionCount = dao.observeFocusSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** True minutes studied since local midnight, so the daily goal tracks real work. */
    private val startOfToday: Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayMinutes = dao.observeFocusMinutesBetween(startOfToday, Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Student-set daily target in minutes. */
    private val goalPrefs = context.getSharedPreferences("focus_goal", Context.MODE_PRIVATE)

    private val _dailyGoalMinutes = MutableStateFlow(goalPrefs.getInt(KEY_GOAL, DEFAULT_GOAL_MINUTES))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    init {
        // Re-arm on launch as well as after edits, so a phone restart or an app
        // upgrade cannot leave an alarm silently unscheduled.
        viewModelScope.launch {
            val enabled = dao.getEnabledAlarms()
            if (enabled.isNotEmpty()) DailyAlarms.rescheduleAll(context, enabled)
        }
    }

    fun selectPreset(minutes: Int) {
        if (_phase.value is FocusPhase.Running) return
        val safe = minutes.coerceIn(5, 120)
        _selectedMinutes.value = safe
        // Keeps the idle countdown honest after the target changes.
        if (_phase.value is FocusPhase.Idle) _remainingSeconds.value = safe * 60
    }

    fun setLabel(label: String) {
        if (_phase.value is FocusPhase.Running) return
        _selectedLabel.value = label
    }

    fun setDailyGoal(minutes: Int) {
        val safe = minutes.coerceIn(15, 480)
        _dailyGoalMinutes.value = safe
        goalPrefs.edit().putInt(KEY_GOAL, safe).apply()
    }

    fun start(minutes: Int = _selectedMinutes.value, label: String = _selectedLabel.value) {
        if (minutes <= 0) return
        if (_phase.value is FocusPhase.Running) return

        startedAt = System.currentTimeMillis()
        _elapsedInRun.value = startedAt
        pausedMillis = 0L
        pausedAt = 0L
        _endsAt.value = startedAt + minutes * 60_000L
        _remainingSeconds.value = minutes * 60
        _phase.value = FocusPhase.Running(minutes, label)
        startTicking()
        notifyStudyStarted(label, minutes)
    }

    fun pause() {
        val current = _phase.value
        if (current !is FocusPhase.Running) return
        tickJob?.cancel()
        tickJob = null
        pausedAt = System.currentTimeMillis()
        _phase.value = FocusPhase.Paused(
            plannedMinutes = current.plannedMinutes,
            label = current.label,
            remainingSeconds = _remainingSeconds.value
        )
    }

    fun resume() {
        val current = _phase.value
        if (current !is FocusPhase.Paused) return
        if (pausedAt > 0L) pausedMillis += System.currentTimeMillis() - pausedAt
        pausedAt = 0L
        _endsAt.value = System.currentTimeMillis() + current.remainingSeconds * 1000L
        _phase.value = FocusPhase.Running(current.plannedMinutes, current.label)
        startTicking()
    }

    /** Stops early and logs however much time was genuinely focused. */
    fun stop() {
        val current = _phase.value
        val planned = when (current) {
            is FocusPhase.Running -> current.plannedMinutes
            is FocusPhase.Paused -> current.plannedMinutes
            else -> return
        }
        val label = when (current) {
            is FocusPhase.Running -> current.label
            is FocusPhase.Paused -> current.label
            else -> return
        }
        tickJob?.cancel()
        tickJob = null
        val endedAt = System.currentTimeMillis()
        if (pausedAt > 0L) pausedMillis += endedAt - pausedAt
        val actual = focusedMinutesBetween(startedAt, endedAt)
        logSession(label, planned, actual, startedAt, endedAt, completed = false)
        _phase.value = FocusPhase.Finished(actual, label)
        clearRun()
    }

    fun discard() {
        tickJob?.cancel()
        tickJob = null
        _phase.value = FocusPhase.Idle
        clearRun()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = ((_endsAt.value - System.currentTimeMillis()) / 1000L)
                    .coerceAtLeast(0L)
                _remainingSeconds.value = remaining.toInt()
                if (remaining <= 0L) break
                delay(1000L)
            }
            completeRun()
        }
    }

    private fun completeRun() {
        val current = _phase.value
        val planned = when (current) {
            is FocusPhase.Running -> current.plannedMinutes
            is FocusPhase.Paused -> current.plannedMinutes
            else -> return
        }
        val label = when (current) {
            is FocusPhase.Running -> current.label
            is FocusPhase.Paused -> current.label
            else -> return
        }
        val endedAt = System.currentTimeMillis()
        val actual = focusedMinutesBetween(startedAt, endedAt)
        logSession(label, planned, planned, startedAt, endedAt, completed = true)
        _phase.value = FocusPhase.Finished(planned, label)
        StudyHubNotifications.showStudyFinished(context, label, planned)
        clearRun()
    }

    /**
     * Minutes genuinely spent running, excluding anything spent paused.
     *
     * Rounded down, matching how a student reads a timer, so a 5 minute block
     * that ends 4m59s early logs 4 rather than silently rounding to 5.
     */
    private fun focusedMinutesBetween(start: Long, end: Long): Int {
        val paused = pausedMillis + if (pausedAt > 0L) end - pausedAt else 0L
        val focused = (end - start - paused).coerceAtLeast(0L)
        return (focused / 60_000L).toInt()
    }

    private fun clearRun() {
        _endsAt.value = 0L
        _elapsedInRun.value = 0L
        startedAt = 0L
        pausedMillis = 0L
        pausedAt = 0L
        _remainingSeconds.value = _selectedMinutes.value * 60
    }

    private fun logSession(
        label: String,
        planned: Int,
        actual: Int,
        started: Long,
        finished: Long,
        completed: Boolean
    ) {
        viewModelScope.launch {
            dao.insertFocusSession(
                FocusSession(
                    label = label,
                    plannedMinutes = planned,
                    actualMinutes = actual,
                    startedAt = started,
                    finishedAt = finished,
                    completed = completed
                )
            )
        }
    }

    /** A heads-up that a focus block started, so the student can leave the app. */
    private fun notifyStudyStarted(label: String, minutes: Int) {
        StudyHubNotifications.showStudyStarted(context, label, minutes)
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { dao.deleteFocusSession(id) }
    }

    /** Clears every session, not just the 30 most recent the screen has loaded. */
    fun clearSessions() {
        viewModelScope.launch { dao.softDeleteAllFocusSessions() }
    }

    // Alarms.

    fun addAlarm(label: String, minuteOfDay: Int, daysMask: Int, vibrate: Boolean, sound: Boolean) {
        // An alarm with no days can never fire, so it is rejected rather than saved.
        if (daysMask == 0) return
        viewModelScope.launch {
            val id = dao.insertAlarm(
                DailyAlarm(
                    label = label,
                    minuteOfDay = minuteOfDay.coerceIn(0, 24 * 60 - 1),
                    daysMask = daysMask,
                    enabled = true,
                    vibrate = vibrate,
                    sound = sound
                )
            )
            val stored = dao.getAlarm(id)
            if (stored != null) DailyAlarms.schedule(context, stored)
        }
    }

    /**
     * Saves an edited alarm and replaces its pending alarm.
     *
     * The old PendingIntent is cancelled first, otherwise AlarmManager keeps two
     * triggers for the same row and the old time still rings.
     */
    fun updateAlarm(alarm: DailyAlarm) {
        if (alarm.daysMask == 0) return
        viewModelScope.launch {
            val previous = dao.getAlarm(alarm.id)
            previous?.let { DailyAlarms.cancel(context, it) }
            dao.updateAlarm(alarm)
            val stored = dao.getAlarm(alarm.id)
            if (stored != null && stored.enabled) DailyAlarms.schedule(context, stored)
        }
    }

    fun toggleAlarm(alarm: DailyAlarm) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = !alarm.enabled)
            dao.updateAlarm(updated)
            DailyAlarms.schedule(context, updated)
        }
    }

    fun deleteAlarm(alarm: DailyAlarm) {
        viewModelScope.launch {
            dao.deleteAlarm(alarm.id)
            DailyAlarms.cancel(context, alarm)
        }
    }

    /** Fires the alarm now so a student can confirm it actually reaches them. */
    fun previewAlarm(alarm: DailyAlarm) {
        DailyAlarms.showNotification(context, alarm.id, alarm.label, alarm.vibrate, alarm.sound)
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val KEY_GOAL = "daily_goal_minutes"
        private const val DEFAULT_GOAL_MINUTES = 120
    }
}
