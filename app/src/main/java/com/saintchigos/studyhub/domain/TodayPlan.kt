package com.saintchigos.studyhub.domain

import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDateTime

/**
 * "What should I do first?" without the student having to work it out.
 *
 * Reads the tasks and exams already in the database and ranks what needs attention:
 * overdue work first, then what is due today, then what is coming in the next few
 * days. Nothing is stored; it is recomputed from the same rows every time, so it can
 * never disagree with the Tasks and Exams screens.
 */
object TodayPlan {

    enum class Urgency { OVERDUE, TODAY, SOON }

    data class Item(
        val title: String,
        val detail: String,
        val urgency: Urgency,
        val colorIndex: Int,
        val isExam: Boolean,
        /** When it is due or starts, used only to order items of equal urgency. */
        val at: Long
    )

    /** How many days ahead still counts as "soon". */
    const val SOON_DAYS = 3L

    fun build(
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: LocalDateTime,
        limit: Int = 4
    ): List<Item> {
        val nowMillis = TimeUtil.toEpochMillis(now)
        val out = ArrayList<Item>()

        for (a in assignments) {
            if (a.isDone) continue
            val code = a.courseCode.ifBlank { a.courseName }
            when {
                a.dueAt < nowMillis -> out.add(
                    Item(
                        title = a.title,
                        detail = "$code  ·  overdue since ${TimeUtil.formatDate(a.dueAt)}",
                        urgency = Urgency.OVERDUE,
                        colorIndex = a.colorIndex,
                        isExam = false,
                        at = a.dueAt
                    )
                )
                TimeUtil.isSameDay(a.dueAt, now) -> out.add(
                    Item(
                        title = a.title,
                        detail = "$code  ·  due today ${TimeUtil.formatTime(a.dueAt)}",
                        urgency = Urgency.TODAY,
                        colorIndex = a.colorIndex,
                        isExam = false,
                        at = a.dueAt
                    )
                )
                TimeUtil.daysUntil(a.dueAt, now) <= SOON_DAYS -> out.add(
                    Item(
                        title = a.title,
                        detail = "$code  ·  due ${TimeUtil.formatDateTime(a.dueAt)}",
                        urgency = Urgency.SOON,
                        colorIndex = a.colorIndex,
                        isExam = false,
                        at = a.dueAt
                    )
                )
            }
        }

        for (e in exams) {
            if (e.startsAt < nowMillis) continue
            val days = TimeUtil.daysUntil(e.startsAt, now)
            if (days > SOON_DAYS) continue
            val code = e.courseCode.ifBlank { e.courseName }
            out.add(
                Item(
                    title = "$code exam",
                    detail = "${e.title}  ·  ${TimeUtil.formatDateTime(e.startsAt)}",
                    urgency = if (TimeUtil.isSameDay(e.startsAt, now)) Urgency.TODAY else Urgency.SOON,
                    colorIndex = e.colorIndex,
                    isExam = true,
                    at = e.startsAt
                )
            )
        }

        return out
            .sortedWith(compareBy<Item> { it.urgency.ordinal }.thenBy { it.at })
            .take(limit)
    }
}
