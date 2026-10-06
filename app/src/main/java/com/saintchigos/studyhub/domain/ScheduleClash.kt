package com.saintchigos.studyhub.domain

import com.saintchigos.studyhub.data.ClassSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Detects when a new timetable entry would sit on top of something already booked.
 *
 * Deliberately pure and free of Android types so it can be unit tested on the JVM
 * with fixed dates, rather than only being checkable by squinting at a real phone.
 *
 * The rule everywhere is the same: two things clash when each one starts before
 * the other ends. Half-open intervals, so a class that ends at 10:00 and an exam
 * starting at 10:00 is not a clash - walking out of one and into the next is fine.
 */

/** A range of minutes from midnight on one particular day. */
data class TimeSpan(val startMinute: Int, val endMinute: Int) {

    /** True when this range and [other] share any time at all. */
    fun overlaps(other: TimeSpan): Boolean =
        startMinute < other.endMinute && other.startMinute < endMinute

    /**
     * True when one ends exactly where the other begins.
     *
     * Worth distinguishing: "back to back" is the common case and is survivable,
     * whereas a real overlap means missing part of a lecture or part of an exam.
     */
    fun isBackToBackWith(other: TimeSpan): Boolean =
        endMinute == other.startMinute || other.endMinute == startMinute
}

/** What the clash is between, so the warning can name it. */
enum class ClashKind { CLASS, EXAM }

data class Clash(val kind: ClashKind, val detail: String)

object ScheduleClash {

    private const val MINUTE_MILLIS = 60_000L

    /**
     * Does an exam at [startsAt] running for [durationMinutes] sit on top of this
     * weekly class?
     *
     * [ClassSession.dayOfWeek] is ISO: 1 is Monday and 7 is Sunday, matching
     * [com.saintchigos.studyhub.data.DailyAlarm].
     */
    fun examVsClass(
        startsAt: Long,
        durationMinutes: Int,
        session: ClassSession,
        zone: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val examSpan = examSpan(minuteOfDay(startsAt, zone), durationMinutes)
        val day = dateAt(startsAt, zone)

        // A class only exists on its own weekday, so anything else cannot clash.
        if (day.dayOfWeek.value != session.dayOfWeek) return false

        return examSpan.overlaps(TimeSpan(session.startMinute, session.endMinute))
    }

    /**
     * Do two exams overlap?
     *
     * Exams are one-off timestamps rather than weekly slots, so this compares real
     * instants and has to account for the two durations.
     */
    fun examVsExam(
        aStartsAt: Long,
        aDurationMinutes: Int,
        bStartsAt: Long,
        bDurationMinutes: Int
    ): Boolean {
        val aStart = aStartsAt
        val aEnd = aStartsAt + aDurationMinutes.coerceAtLeast(0) * MINUTE_MILLIS
        val bStart = bStartsAt
        val bEnd = bStartsAt + bDurationMinutes.coerceAtLeast(0) * MINUTE_MILLIS
        return aStart < bEnd && bStart < aEnd
    }

    /**
     * The exam's start and end as minutes from midnight on its own date.
     *
     * An exam can run past midnight, which is normal for a late sitting. Same-day
     * maths would otherwise put the end before the start and report a clash with
     * the whole timetable, so an overrun is clamped to the end of the day.
     */
    private fun examSpan(startMinute: Int, durationMinutes: Int): TimeSpan {
        val endMinute = startMinute + durationMinutes.coerceAtLeast(0)
        return if (endMinute > MINUTES_IN_DAY) {
            TimeSpan(startMinute, MINUTES_IN_DAY)
        } else {
            TimeSpan(startMinute, endMinute)
        }
    }

    private fun dateAt(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun minuteOfDay(epochMillis: Long, zone: ZoneId): Int {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return time.hour * 60 + time.minute
    }

    /** Human-readable warning for a clash the student is being allowed to keep. */
    fun describe(session: ClassSession, startsAt: Long, durationMinutes: Int, zone: ZoneId): Clash {
        val start = minuteOfDay(startsAt, zone)
        val examSpan = TimeSpan(start, start + durationMinutes.coerceAtLeast(0))
        val classSpan = TimeSpan(session.startMinute, session.endMinute)
        val backToBack = examSpan.isBackToBackWith(classSpan)
        val times = "${formatClock(start)} and ${formatClock(session.startMinute)}-" +
            formatClock(session.endMinute)
        val detail = if (backToBack) {
            "Your exam starts as this class ends ($times). No gap to get there."
        } else {
            "This class runs $times, straight through your exam."
        }
        return Clash(ClashKind.CLASS, detail)
    }

    /** 570 -> "9:30". Used only for the warning text. */
    fun formatClock(minuteOfDay: Int): String {
        val safe = minuteOfDay.coerceIn(0, MINUTES_IN_DAY)
        return "%d:%02d".format(safe / 60, safe % 60)
    }

    private const val MINUTES_IN_DAY = 24 * 60
}