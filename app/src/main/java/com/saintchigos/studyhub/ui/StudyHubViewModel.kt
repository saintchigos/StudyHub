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
import com.saintchigos.studyhub.data.PlanCourse
import com.saintchigos.studyhub.data.PlanWithProgramme
import com.saintchigos.studyhub.data.Programme
import com.saintchigos.studyhub.data.ProgrammePlan
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.reminder.ClassAlarms
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Re-arms reminders whenever the timetable changes. */
    private val timetableWatcher: Job = viewModelScope.launch {
        dao.observeAllSessions().collect { sessions ->
            ClassAlarms.reschedule(getApplication(), sessions)
        }
    }

    val exams: StateFlow<List<ExamWithCourse>> =
        dao.observeUpcomingExams(TimeUtil.toEpochMillis(TimeUtil.now()))
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

    /** Rebuilds every alarm from the current timetable, for example after a settings change. */
    private fun rescheduleNow() {
        viewModelScope.launch(Dispatchers.IO) {
            ClassAlarms.reschedule(getApplication(), dao.getAllSessions())
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