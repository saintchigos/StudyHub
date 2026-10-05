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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val _remainingSeconds = MutableStateFlow(0)

    /** Ticks once a second only while a run is active. */
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _elapsedInRun = MutableStateFlow(0L)
    private var tickJob: Job? = null
    private var startedAt: Long = 0L

    private val _selectedMinutes = MutableStateFlow(25)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _selectedLabel = MutableStateFlow("Deep work")
    val selectedLabel: StateFlow<String> = _selectedLabel.asStateFlow()

    val recentSessions = dao.observeRecentFocusSessions(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals = dao.observeFocusTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalMinutes = dao.observeFocusMinutesTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Minutes focused today, for the streak-style summary on the Focus screen. */
    val todayMinutes: StateFlow<Int> = dao.observeRecentFocusSessions(200)
        .map { sessions ->
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            sessions.filter { it.startedAt >= startOfDay }.sumOf { it.actualMinutes }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val alarms = dao.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        _selectedMinutes.value = minutes
    }

    fun setLabel(label: String) {
        if (_phase.value is FocusPhase.Running) return
        _selectedLabel.value = label
    }

    fun start(minutes: Int = _selectedMinutes.value, label: String = _selectedLabel.value) {
        if (minutes <= 0) return
        if (_phase.value is FocusPhase.Running) return

        startedAt = System.currentTimeMillis()
        _elapsedInRun.value = startedAt
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
        _phase.value = FocusPhase.Paused(
            plannedMinutes = current.plannedMinutes,
            label = current.label,
            remainingSeconds = _remainingSeconds.value
        )
    }

    fun resume() {
        val current = _phase.value
        if (current !is FocusPhase.Paused) return
        _endsAt.value = System.currentTimeMillis() + current.remainingSeconds * 1000L
        _phase.value = FocusPhase.Running(current.plannedMinutes, current.label)
        startTicking()
    }

    /** Stops early and logs whatever was actually done. */
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
        val actual = ((endedAt - _elapsedInRun.value) / 60_000L).toInt().coerceAtLeast(0)
        logSession(label, planned, actual, _elapsedInRun.value, endedAt, completed = false)
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
        logSession(label, planned, planned, _elapsedInRun.value, endedAt, completed = true)
        _phase.value = FocusPhase.Finished(planned, label)
        StudyHubNotifications.showStudyFinished(context, label, planned)
        clearRun()
    }

    private fun clearRun() {
        _endsAt.value = 0L
        _elapsedInRun.value = 0L
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

    // Alarms.

    fun addAlarm(label: String, minuteOfDay: Int, daysMask: Int, vibrate: Boolean, sound: Boolean) {
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
}
