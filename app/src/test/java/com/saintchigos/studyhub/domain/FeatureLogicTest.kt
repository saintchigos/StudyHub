package com.saintchigos.studyhub.domain

import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FeatureLogicTest {

    private val hour = 60L * 60_000L
    private val now = LocalDateTime.of(2026, 10, 9, 8, 0)
    private val nowMs = TimeUtil.toEpochMillis(now)

    private fun task(id: Long, dueAt: Long, done: Boolean = false) = AssignmentWithCourse(
        id = id, title = "Task $id", dueAt = dueAt, isDone = done, priority = 1,
        courseName = "Statistics", courseCode = "STA301", colorIndex = 0
    )

    private fun exam(id: Long, startsAt: Long) = ExamWithCourse(
        id = id, title = "Final", startsAt = startsAt, durationMinutes = 120,
        room = "", notes = "", courseName = "Statistics", courseCode = "STA301", colorIndex = 0
    )

    // ---- today's plan -----------------------------------------------------

    @Test
    fun overdueComesBeforeTodayBeforeSoon() {
        val plan = TodayPlan.build(
            listOf(
                task(1, nowMs + 48 * hour),   // soon
                task(2, nowMs + 5 * hour),    // today
                task(3, nowMs - 30 * hour)    // overdue
            ),
            emptyList(),
            now
        )
        assertEquals(listOf("Task 3", "Task 2", "Task 1"), plan.map { it.title })
        assertEquals(TodayPlan.Urgency.OVERDUE, plan[0].urgency)
    }

    @Test
    fun finishedAndDistantWorkIsLeftOut() {
        val plan = TodayPlan.build(
            listOf(task(1, nowMs + 5 * hour, done = true), task(2, nowMs + 20 * 24 * hour)),
            listOf(exam(1, nowMs + 30 * 24 * hour)),
            now
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun anExamTomorrowIsInThePlanAndTheLimitIsRespected() {
        val tasks = (1L..6L).map { task(it, nowMs - it * hour) }
        val plan = TodayPlan.build(tasks, listOf(exam(9, nowMs + 26 * hour)), now, limit = 4)
        assertEquals(4, plan.size)
        val withExam = TodayPlan.build(emptyList(), listOf(exam(9, nowMs + 26 * hour)), now)
        assertTrue(withExam.single().isExam)
    }

    // ---- study level ------------------------------------------------------

    @Test
    fun levelsStartAtOneAndRiseWithXp() {
        assertEquals(1, StudyLevel.status(0, 0).level)
        assertEquals(1, StudyLevel.status(49, 0).level)
        assertEquals(2, StudyLevel.status(50, 0).level)
        assertEquals(3, StudyLevel.status(200, 0).level)
        assertEquals(4, StudyLevel.status(450, 0).level)
    }

    @Test
    fun sessionsAddABonusAndProgressStaysInRange() {
        assertEquals(60 + 3 * 5, StudyLevel.xp(60, 3))
        val s = StudyLevel.status(125, 0) // halfway from 50 to 200
        assertEquals(2, s.level)
        assertEquals(0.5f, s.progress, 0.001f)
        assertTrue(StudyLevel.status(5_000_000, 0).title.isNotBlank())
    }

    // ---- weekly focus -----------------------------------------------------

    @Test
    fun weeklyFocusSumsPerDayAndMarksToday() {
        fun session(at: LocalDateTime, minutes: Int) = FocusSession(
            label = "x", plannedMinutes = minutes, actualMinutes = minutes,
            startedAt = TimeUtil.toEpochMillis(at), finishedAt = TimeUtil.toEpochMillis(at), completed = true
        )
        val days = WeeklyFocus.lastSevenDays(
            listOf(session(now.withHour(9), 25), session(now.withHour(14), 30), session(now.minusDays(2), 40)),
            now
        )
        assertEquals(7, days.size)
        assertEquals(55, days.last().minutes)
        assertTrue(days.last().isToday)
        assertEquals(40, days[4].minutes)
        assertEquals(0, days.first().minutes)
    }

    // ---- GPA --------------------------------------------------------------

    @Test
    fun gpaIsCreditWeighted() {
        val gpa = GpaCalculator.gpa(
            listOf(GpaCalculator.Entry(4, "A"), GpaCalculator.Entry(2, "C"))
        )
        assertNotNull(gpa)
        assertEquals((4 * 4.0 + 2 * 2.0) / 6, gpa!!, 1e-9)
    }

    @Test
    fun gpaIgnoresUnfilledRowsAndIsNullWhenEmpty() {
        assertNull(GpaCalculator.gpa(emptyList()))
        assertNull(GpaCalculator.gpa(listOf(GpaCalculator.Entry(3, "?"))))
        assertEquals(3.0, GpaCalculator.gpa(listOf(GpaCalculator.Entry(3, "B"), GpaCalculator.Entry(0, "A")))!!, 1e-9)
    }
}
