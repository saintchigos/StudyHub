package com.saintchigos.studyhub.reminder

import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClassReminder(
    val sessionId: Long,
    val date: LocalDate,
    val triggerAtMillis: Long,
    val leadMinutes: Int,
    val session: SessionWithCourse
) {
    val key: String get() = "s${sessionId}_${date}"

    val courseLabel: String get() = "${session.courseCode} · ${session.courseName}"

    val whenLabel: String
        get() = TimeUtil.formatClock(session.startMinute)
}

/**
 * Lead time rule: the first class of a day gets 10 minutes of warning because the
 * student has nothing before it to anchor on. Every later class gets 5 minutes.
 */
object ReminderRules {

    const val FIRST_CLASS_LEAD_MINUTES = 10
    const val LATER_CLASS_LEAD_MINUTES = 5

    fun leadMinutesFor(
        sessionId: Long,
        daySessions: List<SessionWithCourse>,
        firstLead: Int = FIRST_CLASS_LEAD_MINUTES,
        laterLead: Int = LATER_CLASS_LEAD_MINUTES
    ): Int =
        if (daySessions.minByOrNull { it.startMinute }?.sessionId == sessionId) firstLead
        else laterLead

    /**
     * All reminders whose trigger time is still ahead of [now], within [horizonDays].
     * Ordered soonest first.
     */
    fun computeReminders(
        sessions: List<SessionWithCourse>,
        now: LocalDateTime,
        horizonDays: Int = 7,
        limit: Int = 80,
        firstLead: Int = FIRST_CLASS_LEAD_MINUTES,
        laterLead: Int = LATER_CLASS_LEAD_MINUTES
    ): List<ClassReminder> {
        if (sessions.isEmpty()) return emptyList()
        val nowMillis = TimeUtil.toEpochMillis(now)
        val today = now.toLocalDate()
        val byDay = sessions.groupBy { it.dayOfWeek }

        val out = ArrayList<ClassReminder>()
        for (offset in 0 until horizonDays) {
            val date = today.plusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            val daySessions = byDay[dow] ?: continue
            for (s in daySessions) {
                val start = date.atTime(TimeUtil.minuteOfDayToLocalTime(s.startMinute))
                val lead = leadMinutesFor(s.sessionId, daySessions, firstLead, laterLead)
                val trigger = start.minusMinutes(lead.toLong())
                val triggerMillis = TimeUtil.toEpochMillis(trigger)
                if (triggerMillis <= nowMillis) continue
                out.add(
                    ClassReminder(
                        sessionId = s.sessionId,
                        date = date,
                        triggerAtMillis = triggerMillis,
                        leadMinutes = lead,
                        session = s
                    )
                )
            }
            if (out.size >= limit) break
        }
        out.sortBy { it.triggerAtMillis }
        return out.take(limit)
    }

    /**
     * The reminder that should be showing right now, if any. Used for the
     * in-app banner while StudyHub is in the foreground.
     */
    fun imminent(
        sessions: List<SessionWithCourse>,
        now: LocalDateTime,
        windowMillis: Long = 30_000L,
        firstLead: Int = FIRST_CLASS_LEAD_MINUTES,
        laterLead: Int = LATER_CLASS_LEAD_MINUTES
    ): ClassReminder? {
        if (sessions.isEmpty()) return null
        val nowMillis = TimeUtil.toEpochMillis(now)
        val today = now.toLocalDate()
        val dow = now.dayOfWeek.value
        val daySessions = sessions.filter { it.dayOfWeek == dow }

        for (s in daySessions) {
            val start = today.atTime(TimeUtil.minuteOfDayToLocalTime(s.startMinute))
            val lead = leadMinutesFor(s.sessionId, daySessions, firstLead, laterLead)
            val triggerMillis = TimeUtil.toEpochMillis(start.minusMinutes(lead.toLong()))
            if (triggerMillis in (nowMillis - windowMillis)..(nowMillis + windowMillis)) {
                return ClassReminder(
                    sessionId = s.sessionId,
                    date = today,
                    triggerAtMillis = triggerMillis,
                    leadMinutes = lead,
                    session = s
                )
            }
        }
        // Also surface a class that is already under way but just started.
        for (s in daySessions) {
            val start = today.atTime(TimeUtil.minuteOfDayToLocalTime(s.startMinute))
            val startMillis = TimeUtil.toEpochMillis(start)
            if (startMillis in (nowMillis - windowMillis)..nowMillis) {
                return ClassReminder(
                    sessionId = s.sessionId,
                    date = today,
                    triggerAtMillis = startMillis,
                    leadMinutes = 0,
                    session = s
                )
            }
        }
        return null
    }

    private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)

    fun dateLabel(date: LocalDate): String = date.format(dateFmt)

    /**
     * Every key this app could realistically have armed, including dates in the past.
     * Reschedule cancels anything in this space that is no longer wanted, so alarms
     * for deleted or edited classes cannot survive and fire later.
     */
    fun candidateKeys(
        sessions: List<SessionWithCourse>,
        now: LocalDateTime,
        pastDays: Int = 14,
        futureDays: Int = 14
    ): Set<String> {
        val today = now.toLocalDate()
        val out = HashSet<String>()
        for (offset in -pastDays..futureDays) {
            val date = today.plusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            for (s in sessions.filter { it.dayOfWeek == dow }) {
                out.add("s${s.sessionId}_$date")
            }
        }
        return out
    }
}