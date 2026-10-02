package com.saintchigos.studyhub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintchigos.studyhub.data.Assignment
import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ClassSession
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.data.Exam
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.util.TimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyHubViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = StudyHubDatabase.get(app).dao()

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

    val exams: StateFlow<List<ExamWithCourse>> =
        dao.observeUpcomingExams(TimeUtil.toEpochMillis(TimeUtil.now()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        StudyHubDatabase.seedIfEmpty(app, viewModelScope)
    }

    fun toggleAssignmentDone(id: Long, currentDone: Boolean) {
        viewModelScope.launch {
            val existing = dao.getAssignment(id) ?: return@launch
            dao.updateAssignment(existing.copy(isDone = !currentDone))
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