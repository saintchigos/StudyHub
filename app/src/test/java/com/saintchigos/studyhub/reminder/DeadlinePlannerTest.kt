package com.saintchigos.studyhub.reminder

import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.NotificationLog
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DeadlinePlannerTest {

    private val hour = 60L * 60_000L
    private val now = TimeUtil.toEpochMillis(LocalDateTime.of(2026, 10, 9, 8, 0))

    private fun task(id: Long, dueAt: Long, done: Boolean = false) = AssignmentWithCourse(
        id = id, title = "Essay", dueAt = dueAt, isDone = done, priority = 1,
        courseName = "Statistics", courseCode = "STA301", colorIndex = 0
    )

    private fun exam(id: Long, startsAt: Long) = ExamWithCourse(
        id = id, title = "Final", startsAt = startsAt, durationMinutes = 120,
        room = "Hall 2", notes = "", courseName = "Statistics", courseCode = "STA301", colorIndex = 0
    )

    @Test
    fun aTaskDueInTwoDaysGetsBothWarnings() {
        val plan = DeadlinePlanner.plan(listOf(task(1, now + 48 * hour)), emptyList(), now)
        assertEquals(2, plan.size)
        assertEquals(now + 24 * hour, plan[0].triggerAt)
        assertEquals(now + 45 * hour, plan[1].triggerAt)
        assertTrue(plan.all { it.kind == NotificationLog.KIND_TASK })
    }

    @Test
    fun warningsAlreadyInThePastAreSkipped() {
        // Due in 2 hours: the day-ahead and the three-hours-ahead warnings have both gone.
        val plan = DeadlinePlanner.plan(listOf(task(1, now + 2 * hour)), emptyList(), now)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun finishedTasksNeverRemind() {
        val plan = DeadlinePlanner.plan(listOf(task(1, now + 48 * hour, done = true)), emptyList(), now)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun nothingFurtherThanTwoWeeksIsArmed() {
        val plan = DeadlinePlanner.plan(emptyList(), listOf(exam(1, now + 30 * 24 * hour)), now)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun examsWarnADayAndTwoHoursAhead() {
        val plan = DeadlinePlanner.plan(emptyList(), listOf(exam(7, now + 72 * hour)), now)
        assertEquals(listOf("e7_1440", "e7_120"), plan.map { it.key })
        assertTrue(plan.all { it.kind == NotificationLog.KIND_EXAM })
    }

    @Test
    fun remindersComeBackSoonestFirst() {
        val plan = DeadlinePlanner.plan(
            listOf(task(1, now + 100 * hour)),
            listOf(exam(2, now + 30 * hour)),
            now
        )
        assertEquals(plan.map { it.triggerAt }, plan.map { it.triggerAt }.sorted())
    }

    @Test
    fun leadLabelsReadNaturally() {
        assertEquals("24 hours", DeadlinePlanner.leadLabel(24 * 60))
        assertEquals("1 hour", DeadlinePlanner.leadLabel(60))
        assertEquals("90 min", DeadlinePlanner.leadLabel(90))
    }

    @Test
    fun anEmptyDayPostsNoBriefing() {
        assertNull(
            DeadlinePlanner.briefing(emptyList(), emptyList(), emptyList(), LocalDateTime.of(2026, 10, 9, 7, 0))
        )
    }

    @Test
    fun theBriefingSummarisesClassesTasksAndExams() {
        val friday = LocalDateTime.of(2026, 10, 9, 7, 0) // a Friday
        val session = SessionWithCourse(
            sessionId = 1, courseId = 1, courseName = "Statistics", courseCode = "STA301",
            colorIndex = 0, dayOfWeek = friday.dayOfWeek.value, startMinute = 8 * 60,
            endMinute = 9 * 60, room = "A1"
        )
        val dueToday = task(1, TimeUtil.toEpochMillis(friday.withHour(17)))
        val tomorrowExam = exam(2, TimeUtil.toEpochMillis(friday.plusDays(1).withHour(9)))

        val result = DeadlinePlanner.briefing(listOf(session), listOf(dueToday), listOf(tomorrowExam), friday)
        assertNotNull(result)
        assertEquals("Good morning", result!!.first)
        assertTrue(result.second.contains("1 class today"))
        assertTrue(result.second.contains("1 task due today"))
        assertTrue(result.second.contains("exam tomorrow"))
    }
}
