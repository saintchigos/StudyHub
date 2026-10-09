package com.saintchigos.studyhub.reminder

import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.NotificationLog
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDateTime

/**
 * Decides what [DeadlineAlarms] should arm and what the morning briefing says.
 *
 * Pure on purpose: it takes what the database returned plus the current time and
 * returns plain values, so every rule here is unit tested on the JVM without a phone.
 */
object DeadlinePlanner {

    private const val DAY_MS = 24L * 60L * 60_000L
    private const val HORIZON_DAYS = 14L
    private const val MAX_REMINDERS = 60

    /** Minutes before a task is due that a reminder fires: a day ahead, then three hours. */
    val TASK_LEADS = listOf(24 * 60, 3 * 60)

    /** Minutes before an exam: a day ahead for revision, then two hours for the walk there. */
    val EXAM_LEADS = listOf(24 * 60, 2 * 60)

    data class Reminder(
        val key: String,
        val triggerAt: Long,
        val title: String,
        val body: String,
        val kind: String
    )

    /**
     * Every reminder that should currently be armed.
     *
     * Pure, so it can be tested without a phone: it takes what the database returned
     * and the current time, and returns the list. Anything already in the past, already
     * done, or further out than two weeks is left out.
     */
    fun plan(
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: Long
    ): List<Reminder> {
        val horizon = now + HORIZON_DAYS * DAY_MS
        val out = ArrayList<Reminder>()

        for (a in assignments) {
            if (a.isDone) continue
            for (lead in TASK_LEADS) {
                val trigger = a.dueAt - lead * 60_000L
                if (trigger <= now || trigger > horizon) continue
                out.add(
                    Reminder(
                        key = "t${a.id}_$lead",
                        triggerAt = trigger,
                        title = "${a.courseCode.ifBlank { a.courseName }}: ${a.title} is due in ${leadLabel(lead)}",
                        body = "${a.courseName}  ·  due ${TimeUtil.formatDateTime(a.dueAt)}",
                        kind = NotificationLog.KIND_TASK
                    )
                )
            }
        }

        for (e in exams) {
            for (lead in EXAM_LEADS) {
                val trigger = e.startsAt - lead * 60_000L
                if (trigger <= now || trigger > horizon) continue
                val room = if (e.room.isNotBlank()) "  ·  ${e.room}" else ""
                out.add(
                    Reminder(
                        key = "e${e.id}_$lead",
                        triggerAt = trigger,
                        title = "${e.courseCode.ifBlank { e.courseName }} exam in ${leadLabel(lead)}",
                        body = "${e.title}  ·  ${TimeUtil.formatDateTime(e.startsAt)}$room",
                        kind = NotificationLog.KIND_EXAM
                    )
                )
            }
        }

        out.sortBy { it.triggerAt }
        return out.take(MAX_REMINDERS)
    }

    /** "24 hours", "3 hours", "90 min": whichever reads naturally. */
    fun leadLabel(minutes: Int): String = when {
        minutes % 60 == 0 && minutes / 60 == 1 -> "1 hour"
        minutes % 60 == 0 -> "${minutes / 60} hours"
        else -> "$minutes min"
    }

    /**
     * The morning briefing text, or null when there is nothing worth saying.
     *
     * A briefing that says "nothing today" every day teaches the student to swipe it
     * away unread, so an empty day posts nothing at all.
     */
    fun briefing(
        sessions: List<SessionWithCourse>,
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: LocalDateTime
    ): Pair<String, String>? {
        val nowMillis = TimeUtil.toEpochMillis(now)
        val dow = now.dayOfWeek.value
        val today = sessions.filter { it.dayOfWeek == dow }.sortedBy { it.startMinute }
        val dueToday = assignments.filter { !it.isDone && TimeUtil.isSameDay(it.dueAt, now) && it.dueAt >= nowMillis }
        val overdue = assignments.count { !it.isDone && it.dueAt < nowMillis }
        val nextExam = exams.filter { it.startsAt >= nowMillis }.minByOrNull { it.startsAt }
        val examDays = nextExam?.let { TimeUtil.daysUntil(it.startsAt, now) }

        val lines = ArrayList<String>()
        if (today.isNotEmpty()) {
            val first = today.first()
            lines.add(
                "${today.size} ${if (today.size == 1) "class" else "classes"} today, " +
                    "first is ${first.courseCode.ifBlank { first.courseName }} at ${TimeUtil.formatClock(first.startMinute)}"
            )
        }
        if (dueToday.isNotEmpty()) {
            lines.add("${dueToday.size} ${if (dueToday.size == 1) "task" else "tasks"} due today")
        }
        if (overdue > 0) lines.add("$overdue overdue")
        if (nextExam != null && examDays != null && examDays in 0..7) {
            val when_ = when (examDays) {
                0L -> "today"
                1L -> "tomorrow"
                else -> "in $examDays days"
            }
            lines.add("${nextExam.courseCode.ifBlank { nextExam.courseName }} exam $when_")
        }
        if (lines.isEmpty()) return null

        val title = when {
            now.hour < 12 -> "Good morning"
            now.hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
        return title to lines.joinToString("  ·  ")
    }
}
