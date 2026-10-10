package com.saintchigos.studyhub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintchigos.studyhub.data.Assignment
import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.CatalogueJson
import com.saintchigos.studyhub.data.ClassSession
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.data.Exam
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.data.NotificationLog
import com.saintchigos.studyhub.data.PlanCourse
import com.saintchigos.studyhub.data.PlanWithProgramme
import com.saintchigos.studyhub.data.Programme
import com.saintchigos.studyhub.data.ProgrammePlan
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.reminder.ClassAlarms
import com.saintchigos.studyhub.reminder.DeadlineAlarms
import com.saintchigos.studyhub.reminder.ReminderHub
import com.saintchigos.studyhub.reminder.NextClass
import com.saintchigos.studyhub.domain.TimetableText
import com.saintchigos.studyhub.ui.theme.Accent
import com.saintchigos.studyhub.util.Sharing
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyHubViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = StudyHubDatabase.get(app).dao()
    private val prefs = StudyHubPrefs(app)

    val courses: StateFlow<List<Course>> = dao.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentWithCourse>> = dao.observeAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow(TimeUtil.now().dayOfWeek.value)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    fun setDay(day: Int) {
        _selectedDay.value = day
    }

    val sessionsForSelectedDay: StateFlow<List<SessionWithCourse>> =
        _selectedDay
            .flatMapLatest { day -> dao.observeSessionsForDay(day) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every weekly class, used to arm the reminder alarms. */
    val allSessions: StateFlow<List<SessionWithCourse>> = dao.observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * How many reminders the student has not opened yet.
     *
     * Feeds the badge on the bell. Counting here rather than in the screen means
     * the badge is a plain query result, so it cannot drift out of step with the
     * list it is meant to summarise.
     */
    val unreadNotificationCount: StateFlow<Int> = dao.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Recent reminders, newest first, for the inbox behind the bell. */
    val notifications: StateFlow<List<NotificationLog>> = dao.observeNotifications(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Clears the badge. Called when the student opens the inbox. */
    fun markNotificationsRead() = viewModelScope.launch { dao.markAllNotificationsRead() }

    /** Re-arms reminders whenever the timetable changes. */
    private val timetableWatcher: Job = viewModelScope.launch {
        dao.observeAllSessions().collect { sessions ->
            ClassAlarms.reschedule(getApplication(), sessions)
        }
    }

    /**
     * Re-arms deadline reminders whenever a task or exam is added, edited, ticked off
     * or deleted, so a finished task never nags and a new one is covered straight away.
     */
    private val deadlineWatcher: Job = viewModelScope.launch(Dispatchers.IO) {
        combine(
            dao.observeAssignments(),
            dao.observeUpcomingExams(System.currentTimeMillis())
        ) { tasks, upcoming -> DeadlineInputs(tasks, upcoming) }
            .collect { inputs ->
                DeadlineAlarms.reschedule(
                    getApplication(),
                    inputs.tasks.filter { !it.isDone },
                    inputs.exams
                )
                DeadlineAlarms.scheduleBriefing(getApplication())
            }
    }

    val exams: StateFlow<List<ExamWithCourse>> =
        dao.observeUpcomingExams(TimeUtil.toEpochMillis(TimeUtil.now()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- focus numbers for Home -------------------------------------------

    /** Total focus minutes ever logged, which Home turns into a level. */
    val focusMinutesTotal: StateFlow<Int> = dao.observeFocusMinutesTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val focusSessionCount: StateFlow<Int> = dao.observeFocusSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Recent sessions, enough to cover the last seven days for the weekly bars. */
    val recentFocus: StateFlow<List<FocusSession>> = dao.observeRecentFocusSessions(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- programme setup --------------------------------------------------

    val plans: StateFlow<List<PlanWithProgramme>> = dao.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planStats: StateFlow<Map<Long, Pair<Int, Int>>> = dao.observePlanStats()
        .map { list -> list.associate { it.planId to (it.courses to it.sessions) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _setupComplete = MutableStateFlow(prefs.setupComplete)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    // ---- app settings -----------------------------------------------------

    private val _firstClassLead = MutableStateFlow(prefs.firstClassLeadMinutes)
    val firstClassLead: StateFlow<Int> = _firstClassLead.asStateFlow()

    private val _otherClassLead = MutableStateFlow(prefs.otherClassLeadMinutes)
    val otherClassLead: StateFlow<Int> = _otherClassLead.asStateFlow()

    private val _reminderSound = MutableStateFlow(prefs.reminderSound)
    val reminderSound: StateFlow<Boolean> = _reminderSound.asStateFlow()

    private val _reminderVibrate = MutableStateFlow(prefs.reminderVibrate)
    val reminderVibrate: StateFlow<Boolean> = _reminderVibrate.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow(Accent.fromName(prefs.accent))
    val accent: StateFlow<Accent> = _accent.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _amoled = MutableStateFlow(prefs.amoled)
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _fontScale = MutableStateFlow(prefs.fontScale)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _deadlineReminders = MutableStateFlow(prefs.deadlineReminders)
    val deadlineReminders: StateFlow<Boolean> = _deadlineReminders.asStateFlow()

    private val _morningBriefing = MutableStateFlow(prefs.morningBriefing)
    val morningBriefing: StateFlow<Boolean> = _morningBriefing.asStateFlow()

    fun setDeadlineReminders(enabled: Boolean) {
        prefs.deadlineReminders = enabled
        _deadlineReminders.value = enabled
        viewModelScope.launch(Dispatchers.IO) { ReminderHub.rearm(getApplication()) }
    }

    fun setMorningBriefing(enabled: Boolean) {
        prefs.morningBriefing = enabled
        _morningBriefing.value = enabled
    }

    private val _termsAccepted = MutableStateFlow(prefs.termsAccepted)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    fun setFirstClassLead(minutes: Int) {
        prefs.firstClassLeadMinutes = minutes
        _firstClassLead.value = prefs.firstClassLeadMinutes
        rescheduleNow()
    }

    fun setOtherClassLead(minutes: Int) {
        prefs.otherClassLeadMinutes = minutes
        _otherClassLead.value = prefs.otherClassLeadMinutes
        rescheduleNow()
    }

    fun setReminderSound(enabled: Boolean) {
        prefs.reminderSound = enabled
        _reminderSound.value = enabled
    }

    fun setReminderVibrate(enabled: Boolean) {
        prefs.reminderVibrate = enabled
        _reminderVibrate.value = enabled
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    /**
     * Switching accent, wallpaper colours, AMOLED or text size all collapse onto the
     * accent picker being irrelevant: taking the wallpaper's colours means the
     * student's chosen accent is ignored, so it is hidden rather than left looking
     * broken when they come back.
     */
    fun setAccent(value: Accent) {
        prefs.accent = value.name
        _accent.value = value
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.dynamicColor = enabled
        _dynamicColor.value = enabled
    }

    fun setAmoled(enabled: Boolean) {
        prefs.amoled = enabled
        _amoled.value = enabled
    }

    fun setFontScale(scale: Float) {
        prefs.fontScale = scale
        _fontScale.value = prefs.fontScale
    }

    fun acceptTerms() {
        prefs.termsAccepted = true
        _termsAccepted.value = true
    }

    private val _showStudyTips = MutableStateFlow(prefs.showStudyTips)
    val showStudyTips: StateFlow<Boolean> = _showStudyTips.asStateFlow()

    fun setShowStudyTips(enabled: Boolean) {
        prefs.showStudyTips = enabled
        _showStudyTips.value = enabled
    }

    /**
     * Consecutive days the student opened the app or ticked off work.
     *
     * Cheap to compute and needs no storage beyond one day number, which matters on a
     * phone where every extra table is a migration.
     */
    private val _studyStreak = MutableStateFlow(0)
    val studyStreak: StateFlow<Int> = _studyStreak.asStateFlow()

    private val _showStreakBanner = MutableStateFlow(false)
    val showStreakBanner: StateFlow<Boolean> = _showStreakBanner.asStateFlow()

    fun dismissStreakBanner() {
        _showStreakBanner.value = false
    }

    /** Called when the student completes work, which is what actually counts as study. */
    fun noteStudyActivity() {
        val today = TimeUtil.toEpochMillis(TimeUtil.now().toLocalDate().atStartOfDay()) / 86_400_000L
        val last = prefs.lastActiveDay
        val next = when (last) {
            today -> prefs.streakDays
            today - 1 -> prefs.streakDays + 1
            else -> 1
        }
        prefs.lastActiveDay = today
        prefs.streakDays = next
        _studyStreak.value = next
        _showStreakBanner.value = true
    }

    fun sendTestReminder() {
        ClassAlarms.showTestNotification(getApplication())
    }

    /**
 * Rebuilds every alarm from the current timetable, for example after a settings change.
 *
 * The "what's next" notification rides along here rather than having its own timer,
 * because the same edit is the only thing that can invalidate it. A separate poll
 * would wake the radio for no new information.
 */
private fun rescheduleNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = dao.getAllSessions()
            ClassAlarms.reschedule(getApplication(), sessions)
            refreshNextClass(sessions)
        }
    }

    /**
     * Reposts or clears the silent "what's next" card, honouring the student's switch.
     *
     * Public because the card also has to catch up on resume: a timetable can go
     * stale overnight, or a class can start while the app sits in the background.
     */
    fun refreshNextClass(sessions: List<com.saintchigos.studyhub.data.SessionWithCourse> = allSessions.value) {
        val app = getApplication<android.app.Application>()
        if (prefs.nextClassNotification) {
            NextClass.update(app, sessions)
        } else {
            NextClass.cancel(app)
        }
    }

    fun setNextClassNotification(enabled: Boolean) {
        prefs.nextClassNotification = enabled
        _nextClassNotification.value = enabled
        viewModelScope.launch(Dispatchers.IO) { refreshNextClass() }
    }

    private val _nextClassNotification = MutableStateFlow(prefs.nextClassNotification)
    val nextClassNotification: StateFlow<Boolean> = _nextClassNotification.asStateFlow()

/**
 * The programme the student is in, for labelling shared exports.
 *
 * Null when no plan has been applied, so the caller can leave the heading off rather
 * than printing an empty one.
 */
private fun currentProgrammeName(): String? {
        val plan = plans.value.firstOrNull { it.applied } ?: return null
        return "${plan.programmeName}, Year ${plan.year}, Semester ${plan.semester}"
    }

    /**
     * Builds the plain text timetable and hands it to the system share sheet.
 *
 * Done in the ViewModel so the screen stays free of formatting, and so the message can
 * be produced from the same data the timetable screen is already holding rather than
 * a second read.
 */
fun shareTimetable(onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = dao.getAllSessions()
            val entries = sessions.map {
                TimetableText.Entry(
                    courseName = it.courseName,
                    courseCode = it.courseCode,
                    dayOfWeek = it.dayOfWeek,
                    startMinute = it.startMinute,
                    endMinute = it.endMinute,
                    room = it.room
                )
            }
            val programme = currentProgrammeName()
            val text = TimetableText.format(entries, programme)
            val shared = withContext(Dispatchers.Main) {
                Sharing.shareText(getApplication(), "My timetable", text)
            }
            onReady(
                when {
                    entries.isEmpty() -> "Nothing scheduled yet, so there was nothing to share."
                    !shared -> "Could not open the share sheet on this phone."
                    else -> "Timetable ready to share."
                }
            )
        }
    }

    /** Builds a shareable catalogue file from what is in this device. */
    fun exportCatalogue(contributor: String?, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = contributor?.trim()?.ifEmpty { null }
            val seeds = StudyHubDatabase.exportSeeds(dao)
            if (seeds.isEmpty()) return@launch
            onReady(CatalogueJson.build(seeds, name))
        }
    }

    /**
     * Merges a catalogue file from a beta tester. Existing programmes are updated in
     * place by slug, so importing the same file twice is harmless and the student's
     * own courses, timetable, assignments and exams are never touched.
     */
    fun importCatalogue(raw: String, onResult: (ImportSummary) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = runCatching {
                val seeds = CatalogueJson.parse(raw)
                if (seeds.isEmpty()) throw IllegalArgumentException("No programmes found in that file.")
                val before = dao.countProgrammes()
                StudyHubDatabase.mergeCatalogue(dao, seeds)
                val after = dao.countProgrammes()
                ImportSummary(
                    programmes = after - before,
                    plans = seeds.size,
                    courses = seeds.sumOf { it.courses.size },
                    sessions = seeds.sumOf { seed -> seed.courses.sumOf { it.sessions.size } }
                )
            }.getOrElse { error ->
                ImportSummary(error = error.message ?: "That file could not be read.")
            }
            onResult(summary)
        }
    }

    data class ImportSummary(
        val programmes: Int = 0,
        val plans: Int = 0,
        val courses: Int = 0,
        val sessions: Int = 0,
        val error: String? = null
    )

    /** Wipes every student record. The programme catalogue is kept so setup still works. */
    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllSessions()
            dao.deleteAllAssignments()
            dao.deleteAllExams()
            dao.deleteAllCourses()
            dao.clearAppliedPlans()
            ClassAlarms.cancelAll(getApplication())
            restartSetup()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // The bundled catalogue is merged on every launch: offline-first, and a
            // released update adds new programmes without duplicating existing ones.
            StudyHubDatabase.mergeCatalogue(dao, StudyHubDatabase.readBundledCatalogue(app))
            adoptExistingSetup()
            restoreStreak()
        }
    }

    /** Rebuilds the streak from the stored day number, so it survives a restart. */
    private fun restoreStreak() {
        val today = TimeUtil.toEpochMillis(TimeUtil.now().toLocalDate().atStartOfDay()) / 86_400_000L
        val last = prefs.lastActiveDay
        _studyStreak.value = when (last) {
            today, today - 1 -> prefs.streakDays
            else -> 0
        }
    }

    /**
     * Students who already had a timetable before programmes existed keep their data
     * and are matched to the closest catalogue plan instead of being sent back to
     * the start-up page.
     */
    private suspend fun adoptExistingSetup() {
        withContext(Dispatchers.IO) {
            val myCodes = dao.getCourses().map { it.code }.toSet()
            if (myCodes.isEmpty()) return@withContext

            val applied = dao.getAppliedPlanIds()
            if (applied.isEmpty()) {
                var best: Pair<Long, Int>? = null
                val candidates = dao.getAllPlans()
                for (plan in candidates) {
                    val planCodes = dao.getPlanCourses(plan.id).map { it.code }.toSet()
                    if (planCodes.isEmpty()) continue
                    val overlap = planCodes.count { it in myCodes }
                    if (overlap > (best?.second ?: 0)) best = plan.id to overlap
                }
                val threshold = 1
                if (best != null && best.second >= threshold) {
                    StudyHubDatabase.applyPlan(dao, best.first)
                }
            }
            prefs.setupComplete = true
            _setupComplete.value = true
        }
    }

    fun completeSetup() {
        prefs.setupComplete = true
        _setupComplete.value = true
    }

    /** Sends the user back to the start-up picker without deleting anything. */
    fun restartSetup() {
        prefs.setupComplete = false
        _setupComplete.value = false
    }

    fun applyPlan(planId: Long) {
        viewModelScope.launch { StudyHubDatabase.applyPlan(dao, planId) }
    }

    fun removePlan(planId: Long) {
        viewModelScope.launch { StudyHubDatabase.removePlan(dao, planId) }
    }

    /**
     * Ends the current semester. Class times and reminders for the applied plan are
     * cleared but the courses stay, so old assignments and exams are still readable.
     */
    fun endSemester(planId: Long) {
        viewModelScope.launch { StudyHubDatabase.removePlan(dao, planId, keepCourses = true) }
    }

    fun addProgramme(name: String, year: Int, semester: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val programmeId = dao.insertProgramme(Programme(name = name.trim(), isCustom = true))
            dao.insertPlan(ProgrammePlan(programmeId = programmeId, year = year, semester = semester))
        }
    }

    fun addPlanCourse(planId: Long, name: String, code: String, credits: Int) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlanCourses(
                listOf(
                    PlanCourse(
                        planId = planId,
                        name = name.trim(),
                        code = code.trim().uppercase().ifBlank { "CRS" },
                        credits = credits.coerceIn(1, 12),
                        colorIndex = dao.countPlanCourses(planId) % 8
                    )
                )
            )
        }
    }

    fun toggleAssignmentDone(id: Long, currentDone: Boolean) {
        viewModelScope.launch {
            val existing = dao.getAssignment(id) ?: return@launch
            dao.updateAssignment(existing.copy(isDone = !currentDone))
            // Marking work off is real study, so it feeds the streak.
            if (!currentDone) noteStudyActivity()
        }
    }

    fun addCourse(name: String, code: String, credits: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val colorIndex = courses.value.size % 8
            dao.insertCourse(
                Course(
                    name = name.trim(),
                    code = code.trim().uppercase().ifBlank { "CRS" },
                    credits = credits.coerceIn(1, 12),
                    colorIndex = colorIndex
                )
            )
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { dao.deleteCourse(course) }
    }

    fun addSession(
        courseId: Long,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
        room: String
    ) {
        if (courseId <= 0 || endMinute <= startMinute) return
        viewModelScope.launch {
            dao.insertSessions(
                listOf(
                    ClassSession(
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        room = room.trim()
                    )
                )
            )
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { dao.deleteSession(id) }
    }

    /**
     * Updates a weekly class. Room rewrites the row so the @Update matches,
     * and a zero id would otherwise create a duplicate row.
     */
    fun editSession(
        id: Long,
        courseId: Long,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
        room: String
    ) {
        if (id <= 0 || courseId <= 0 || endMinute <= startMinute) return
        viewModelScope.launch {
            val existing = dao.getSession(id) ?: return@launch
            if (existing.courseId != courseId) {
                dao.deleteSession(id)
                dao.insertSessions(
                    listOf(
                        ClassSession(
                            courseId = courseId,
                            dayOfWeek = dayOfWeek,
                            startMinute = startMinute,
                            endMinute = endMinute,
                            room = room.trim()
                        )
                    )
                )
            } else {
                dao.updateSession(
                    existing.copy(
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        room = room.trim()
                    )
                )
            }
        }
    }

    fun addAssignment(courseId: Long, title: String, dueAt: Long, priority: Int) {
        if (courseId <= 0 || title.isBlank()) return
        viewModelScope.launch {
            dao.insertAssignment(
                Assignment(
                    courseId = courseId,
                    title = title.trim(),
                    dueAt = dueAt,
                    priority = priority.coerceIn(0, 2)
                )
            )
        }
    }

    fun deleteAssignment(id: Long) {
        viewModelScope.launch { dao.deleteAssignment(id) }
    }

    fun addExam(
        courseId: Long,
        title: String,
        startsAt: Long,
        durationMinutes: Int,
        room: String,
        notes: String
    ) {
        if (courseId <= 0 || title.isBlank()) return
        viewModelScope.launch {
            dao.insertExam(
                Exam(
                    courseId = courseId,
                    title = title.trim(),
                    startsAt = startsAt,
                    durationMinutes = durationMinutes.coerceIn(15, 480),
                    room = room.trim(),
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteExam(id: Long) {
        viewModelScope.launch { dao.deleteExam(id) }
    }
}

/** What the deadline watcher needs from the database, kept as one value so it re-arms once per change. */
private data class DeadlineInputs(
    val tasks: List<AssignmentWithCourse>,
    val exams: List<ExamWithCourse>
)
