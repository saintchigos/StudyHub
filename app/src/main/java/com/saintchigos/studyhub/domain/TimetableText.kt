package com.saintchigos.studyhub.domain

/**
 * Renders the timetable as plain text a student can paste into a group chat.
 *
 * Students constantly need to answer "what time is your lecture?" in a WhatsApp
 * group, and the honest answer is that a screenshot is hard to read on a phone and a
 * file is worse. Plain text sorts, wraps and reads in any chat app, works with no
 * app installed on the other side, and costs no data on a metered connection, which
 * matters more here than a prettier format.
 *
 * Deliberately free of Android and Compose types so the formatting is unit tested on
 * the JVM rather than only being checkable by sharing it on a real phone.
 */
object TimetableText {

    /** Days in the order a timetable is read, Monday first. */
    private val DAY_ORDER = listOf(1, 2, 3, 4, 5, 6, 7)

    private val DAY_NAMES = mapOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday"
    )

    /** A row of the export: one class at one time. */
    data class Entry(
        val courseName: String,
        val courseCode: String,
        val dayOfWeek: Int,
        val startMinute: Int,
        val endMinute: Int,
        val room: String
    )

    /**
     * Builds the message body.
     *
     * Grouped by day and sorted by start time within each day, because that is how a
     * person reads a timetable and how they will scan it in a chat. Days with nothing
     * on are dropped entirely rather than listed as empty, so the message stays short
     * and does not imply they are free all day.
     */
    fun format(entries: List<Entry>, programmeName: String? = null): String {
        val builder = StringBuilder()

        val header = if (programmeName.isNullOrBlank()) "My timetable" else "My timetable - $programmeName"
        builder.appendLine(header)
        builder.appendLine()

        val populated = DAY_ORDER.filter { day -> entries.any { it.dayOfWeek == day } }
        if (populated.isEmpty()) return "$header\n\nNo classes scheduled yet."

        populated.forEach { day ->
            val forDay = entries
                .filter { it.dayOfWeek == day }
                .sortedWith(compareBy({ it.startMinute }, { it.endMinute }))

            builder.appendLine("${DAY_NAMES.getValue(day)}:")

            // Padded to a fixed width so the times line up down the message. Chat apps
            // render proportional fonts, so this cannot be perfect, but it is closer
            // than a ragged list and degrades gracefully.
            forDay.forEach { entry ->
                val time = "${timeOfDay(entry.startMinute)} - ${timeOfDay(entry.endMinute)}"
                val code = if (entry.courseCode.isBlank()) entry.courseName else entry.courseCode
                val room = if (entry.room.isBlank()) "" else "  ${entry.room}"
                builder.appendLine("  $time  $code$room")
            }
            builder.appendLine()
        }

        builder.appendLine("Sent from StudyHub")
        return builder.toString().trimEnd()
    }

    /** "09:00" from minutes past midnight, so 540 becomes 09:00. */
    fun timeOfDay(minuteOfDay: Int): String {
        val safe = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        val h = safe / 60
        val m = safe % 60
        return "%02d:%02d".format(h, m)
    }
}