package com.saintchigos.studyhub.util

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtil {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun now(): LocalDateTime = LocalDateTime.now(zone)

    fun minuteOfDayToLocalTime(minute: Int): LocalTime =
        LocalTime.of((minute / 60).coerceIn(0, 23), (minute % 60).coerceIn(0, 59))

    fun formatClock(minute: Int): String {
        val t = minuteOfDayToLocalTime(minute)
        val h = when {
            t.hour == 0 || t.hour == 12 -> 12
            else -> t.hour % 12
        }
        val suffix = if (t.hour < 12) "AM" else "PM"
        return String.format(Locale.US, "%d:%02d %s", h, t.minute, suffix)
    }

    fun formatClock24(minute: Int): String {
        val t = minuteOfDayToLocalTime(minute)
        return String.format(Locale.US, "%02d:%02d", t.hour, t.minute)
    }

    fun dayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }

    fun shortDayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        else -> "Sun"
    }

    /** Maps java.time DayOfWeek (Mon=1..Sun=7) to our stored dayOfWeek (Mon=1..Sun=7). */
    fun fromDayOfWeek(d: DayOfWeek): Int = d.value

    fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    fun toEpochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun formatDateTime(epochMillis: Long): String =
        toLocalDateTime(epochMillis).format(dateTimeFormatter)

    fun formatDate(epochMillis: Long): String =
        toLocalDateTime(epochMillis).format(dateFormatter)

    fun formatTime(epochMillis: Long): String =
        toLocalDateTime(epochMillis).format(timeFormatter)

    /**
     * Human countdown such as "in 3 days", "in 4h 20m", "overdue by 2h".
     */
    fun relativeLabel(epochMillis: Long, reference: LocalDateTime = now()): String {
        val duration = Duration.between(reference, toLocalDateTime(epochMillis))
        val abs = duration.abs()
        if (abs.seconds < 60) return "now"
        return if (duration.isNegative) "overdue by ${magnitude(abs)}" else "in ${magnitude(abs)}"
    }

    /**
     * How long ago something happened, such as "just now", "4 min ago", "3 days ago".
     *
     * For things that already happened rather than deadlines still to meet, so
     * a reminder that arrived a minute ago never reads as "overdue by 1 min".
     */
    fun agoLabel(epochMillis: Long, reference: LocalDateTime = now()): String {
        val abs = Duration.between(reference, toLocalDateTime(epochMillis)).abs()
        if (abs.seconds < 60) return "just now"
        return "${magnitude(abs)} ago"
    }

    /** The size of a gap on its own, without saying whether it is ahead or behind. */
    private fun magnitude(abs: Duration): String {
        val days = abs.toDays()
        val hours = abs.toHours() % 24
        val minutes = abs.toMinutes() % 60

        return when {
            days >= 1 && days < 7 -> "$days day${plural(days)}"
            days >= 7 -> "${days / 7} week${plural(days / 7)}"
            hours >= 1 -> "$hours h${if (minutes > 0) " $minutes m" else ""}"
            else -> "$minutes min"
        }
    }

    private fun plural(n: Long) = if (n == 1L) "" else "s"

    /** True when the deadline falls on the calendar day of [reference]. */
    fun isSameDay(epochMillis: Long, reference: LocalDateTime = now()): Boolean =
        toLocalDateTime(epochMillis).toLocalDate() == reference.toLocalDate()

    fun daysUntil(epochMillis: Long, reference: LocalDateTime = now()): Long =
        Duration.between(
            reference.toLocalDate().atStartOfDay(),
            toLocalDateTime(epochMillis).toLocalDate().atStartOfDay()
        ).toDays()

    fun today(): LocalDate = now().toLocalDate()

    /**
     * Next occurrence of a weekly session, as minutes from now.
     * Used to tell the student whether a class is "in 2h" today.
     */
    fun minutesUntilSession(dayOfWeek: Int, startMinute: Int, reference: LocalDateTime = now()): Long {
        val today = reference.toLocalDate()
        val todayIso = today.dayOfWeek.value
        var deltaDays = (dayOfWeek - todayIso + 7) % 7
        val todayStart = reference.toLocalTime()
        val sessionTime = minuteOfDayToLocalTime(startMinute)
        if (deltaDays == 0 && !sessionTime.isAfter(todayStart)) {
            deltaDays = 7
        }
        val target = today.plusDays(deltaDays.toLong())
            .atTime(sessionTime)
        return Duration.between(reference, target).toMinutes()
    }
}