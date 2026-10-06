package com.saintchigos.studyhub.reminder

import com.saintchigos.studyhub.data.SessionWithCourse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * Pins which class the lock screen card advertises.
 *
 * Getting this wrong is worse than not having the card: pointing a student at a class
 * that already started sends them to the wrong lecture on a morning they are already
 * late for.
 */
class NextClassTest {

    private fun session(day: Int, start: Int, code: String = "X") = SessionWithCourse(
        sessionId = 1,
        courseId = 1,
        courseName = "Course $code",
        courseCode = code,
        colorIndex = 0,
        dayOfWeek = day,
        startMinute = start,
        endMinute = start + 60,
        room = "A12"
    )

    // 2026-10-06 is a Tuesday, ISO day 2.
    private val tuesdayMorning = LocalDateTime.of(2026, 10, 6, 8, 0)

    @Test
    fun picksTheSoonestClassLaterToday() {
        val next = NextClass.nextSession(
            listOf(
                session(2, 9 * 60, "LATER"),
                session(2, 10 * 60, "LATEST")
            ),
            tuesdayMorning
        )
        assertEquals("LATER", next?.first?.courseCode)
        assertEquals(60L, next?.second)
    }

    @Test
    fun skipsAClassThatHasAlreadyStarted() {
        // 08:00 now, so the 07:00 lecture is history, not the next thing.
        val next = NextClass.nextSession(
            listOf(session(2, 7 * 60, "OVER"), session(2, 14 * 60, "AFTERNOON")),
            tuesdayMorning
        )
        assertEquals("AFTERNOON", next?.first?.courseCode)
    }

    @Test
    fun aClassThatAlreadyFinishedIsNotOfferedAsNext() {
        // Today's 07:00 lecture ended at 08:00. It is not something to walk to, and
        // saying "Calculus in 6 days" on a Tuesday morning is noise, not help. The
        // card's answer here is "no more classes today" instead.
        assertNull(NextClass.nextSession(listOf(session(2, 7 * 60, "MON")), tuesdayMorning))
    }

    @Test
    fun tomorrowIsNextEvenWhenTodaysIsOver() {
        // The finished 07:00 is skipped, but the Wednesday lecture is still the next
        // thing to be at, so the card keeps saying something useful.
        val next = NextClass.nextSession(
            listOf(session(2, 7 * 60, "OVER"), session(3, 9 * 60, "WED")),
            tuesdayMorning
        )
        assertEquals("WED", next?.first?.courseCode)
    }

    @Test
    fun aClassInProgressIsNotTreatedAsNext() {
        // 08:30, and this lecture runs 08:00 to 09:00. You are already in it.
        val during = LocalDateTime.of(2026, 10, 6, 8, 30)
        assertNull(NextClass.nextSession(listOf(session(2, 8 * 60, "NOW")), during))
    }

    @Test
    fun movesForwardToTheNextDayRatherThanBackwards() {
        val next = NextClass.nextSession(listOf(session(3, 9 * 60, "WED")), tuesdayMorning)
        assertEquals("WED", next?.first?.courseCode)
        // A day and an hour away, not an hour: the point of the test is that it does
        // not treat tomorrow as sooner than today.
        assertEquals(24 * 60 + 60L, next?.second)
    }

    @Test
    fun wrapsAroundTheEndOfTheWeek() {
        // Sunday is day 7, and it must not be read as coming before Tuesday.
        val next = NextClass.nextSession(listOf(session(7, 9 * 60, "SUN")), tuesdayMorning)
        assertEquals("SUN", next?.first?.courseCode)
        assertEquals(5 * 24 * 60 + 60L, next?.second)
    }

    @Test
    fun returnsNullWhenNothingIsLeftTodayOrLaterThisWeek() {
        // Only a Tuesday class, and it has already run.
        assertNull(NextClass.nextSession(listOf(session(2, 7 * 60)), tuesdayMorning))
    }

    @Test
    fun returnsNullForAnEmptyTimetable() {
        assertNull(NextClass.nextSession(emptyList(), tuesdayMorning))
    }

    @Test
    fun aClassStartingExactlyNowCountsAsStartedNotNext() {
        val now = LocalDateTime.of(2026, 10, 6, 9, 0)
        // Half open interval, same as the clash check: a class you are already inside
        // is not something to walk to.
        assertNull(NextClass.nextSession(listOf(session(2, 9 * 60, "NOW")), now))
    }

    @Test
    fun picksTheSoonestAcrossAWholeWeek() {
        val next = NextClass.nextSession(
            listOf(
                session(7, 9 * 60, "SUNDAY"),
                session(4, 9 * 60, "THURSDAY"),
                session(3, 9 * 60, "WEDNESDAY")
            ),
            tuesdayMorning
        )
        assertEquals("WEDNESDAY", next?.first?.courseCode)
    }

    @Test
    fun minuteAccuracyIsPreserved() {
        val next = NextClass.nextSession(listOf(session(2, 9 * 60 + 30)), tuesdayMorning)
        assertEquals(90L, next?.second)
    }
}