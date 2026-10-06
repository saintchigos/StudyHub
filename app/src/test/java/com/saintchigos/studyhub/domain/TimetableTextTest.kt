package com.saintchigos.studyhub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the shared timetable text.
 *
 * The output goes into a group chat, so the things that would embarrass a student are
 * worth a test: classes landing on the wrong day, times that are not readable, and a
 * message padded out with empty days.
 */
class TimetableTextTest {

    private fun entry(
        code: String,
        day: Int,
        start: Int,
        end: Int,
        room: String = "A12",
        name: String = "Course $code"
    ) = TimetableText.Entry(name, code, day, start, end, room)

    @Test
    fun timesAreRenderedAsTwentyFourHourClock() {
        assertEquals("09:00", TimetableText.timeOfDay(540))
        assertEquals("00:00", TimetableText.timeOfDay(0))
        assertEquals("23:59", TimetableText.timeOfDay(1439))
        assertEquals("13:30", TimetableText.timeOfDay(810))
    }

    @Test
    fun timesOutsideTheDayAreClampedRatherThanPrintingNonsense() {
        // A bad time in the database must not produce "25:99" in a message a student
        // sends to other people.
        assertEquals("00:00", TimetableText.timeOfDay(-30))
        assertEquals("23:59", TimetableText.timeOfDay(9999))
    }

    @Test
    fun eachClassLandsUnderTheRightDay() {
        val text = TimetableText.format(
            listOf(
                entry("MATH101", day = 1, start = 540, end = 630),
                entry("CS2100", day = 3, start = 780, end = 870)
            )
        )
        assertTrue("Monday heading missing", text.contains("Monday:"))
        assertTrue("Wednesday heading missing", text.contains("Wednesday:"))

        val monday = text.substringAfter("Monday:").substringBefore("Wednesday:")
        assertTrue("Math landed on the wrong day", monday.contains("MATH101"))
        assertTrue("CS leaked onto Monday", !monday.contains("CS2100"))
    }

    @Test
    fun classesAreSortedByStartTimeWithinADay() {
        val text = TimetableText.format(
            listOf(
                entry("LATE", day = 2, start = 900, end = 960),
                entry("EARLY", day = 2, start = 480, end = 540)
            )
        )
        assertTrue("not in time order", text.indexOf("EARLY") < text.indexOf("LATE"))
    }

    @Test
    fun emptyDaysAreLeftOutEntirely() {
        val text = TimetableText.format(listOf(entry("MATH101", day = 1, start = 540, end = 630)))
        assertTrue("listed an empty Saturday", !text.contains("Saturday"))
        assertTrue("listed an empty Sunday", !text.contains("Sunday"))
    }

    @Test
    fun anEmptyTimetableSaysSoRatherThanShowingAColon() {
        val text = TimetableText.format(emptyList())
        assertTrue("did not explain the empty case: $text", text.contains("No classes scheduled"))
        assertTrue("left a dangling heading", !text.contains(":"))
    }

    @Test
    fun roomIsOmittedWhenThereIsNotOne() {
        val withRoom = TimetableText.format(listOf(entry("A", 1, 540, 630, room = "LT4")))
        assertTrue("room missing", withRoom.contains("LT4"))

        val without = TimetableText.format(listOf(entry("B", 1, 540, 630, room = "")))
        assertTrue("left trailing space", !without.contains("  \n"))
        assertTrue("still shows the code", without.contains("B"))
    }

    @Test
    fun courseNameIsUsedWhenThereIsNoCode() {
        val text = TimetableText.format(
            listOf(TimetableText.Entry("Wide Engineering", "", 1, 540, 630, ""))
        )
        assertTrue("fell back to nothing: $text", text.contains("Wide Engineering"))
    }

    @Test
    fun programmeNameOnlyAppearsWhenKnown() {
        // Checked on the heading line only: the body legitimately contains " - " as the
        // separator in each time range.
        fun heading(text: String) = text.lineSequence().first()

        assertEquals("My timetable", heading(TimetableText.format(listOf(entry("A", 1, 540, 630)))))
        assertEquals(
            "My timetable",
            heading(TimetableText.format(listOf(entry("A", 1, 540, 630)), "   "))
        )
        assertEquals(
            "My timetable - Computers and Statistics, Year 3, Semester A",
            heading(
                TimetableText.format(
                    listOf(entry("A", 1, 540, 630)),
                    "Computers and Statistics, Year 3, Semester A"
                )
            )
        )
    }

    @Test
    fun everyDayOfTheWeekHasAName() {
        // A missing map entry would throw at format time and break sharing outright.
        listOf(
            entry("A", 1, 540, 630),
            entry("B", 2, 540, 630),
            entry("C", 3, 540, 630),
            entry("D", 4, 540, 630),
            entry("E", 5, 540, 630),
            entry("F", 6, 540, 630),
            entry("G", 7, 540, 630)
        ).forEach { assertTrue("day ${it.dayOfWeek} missing", it.dayOfWeek in 1..7) }

        val text = TimetableText.format(
            listOf(
                entry("A", 1, 540, 630), entry("B", 2, 540, 630), entry("C", 3, 540, 630),
                entry("D", 4, 540, 630), entry("E", 5, 540, 630), entry("F", 6, 540, 630),
                entry("G", 7, 540, 630)
            )
        )
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            .forEach { assertTrue("$it missing from $text", text.contains(it)) }
    }
}