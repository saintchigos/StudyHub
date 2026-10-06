package com.saintchigos.studyhub.domain

import com.saintchigos.studyhub.data.ClassSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Clash detection is the kind of logic that looks obvious and is quietly wrong at
 * the edges, so the edges are what get tested: midnight, midnight-after, an exam
 * that runs long, and a class that ends exactly when the exam begins.
 */
class ScheduleClashTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 5)

    /** Monday, so a Monday class can clash. */
    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(monday, LocalTime.of(hour, minute))
            .atZone(zone).toInstant().toEpochMilli()

    private fun mondayClass(start: Int, end: Int) =
        ClassSession(courseId = 1, dayOfWeek = 1, startMinute = start, endMinute = end)

    @Test
    fun examInsideALectureIsAClash() {
        // Lecture 09:00-10:30, exam 10:00 for two hours.
        assertTrue(
            ScheduleClash.examVsClass(at(10), 120, mondayClass(9 * 60, 10 * 60 + 30), zone)
        )
    }

    @Test
    fun examStartingWhenTheLectureFinishesIsNotAClash() {
        // Half-open intervals: 09:00-10:00 then an exam at 10:00 is fine.
        assertFalse(
            ScheduleClash.examVsClass(at(10), 60, mondayClass(9 * 60, 10 * 60), zone)
        )
    }

    @Test
    fun examEndingWhenTheLectureStartsIsNotAClash() {
        // Exam 08:00-09:00, lecture 09:00 onwards.
        assertFalse(
            ScheduleClash.examVsClass(at(8), 60, mondayClass(9 * 60, 10 * 60), zone)
        )
    }

    @Test
    fun aClassOnAnotherDayNeverClashes() {
        // Same clock time, but the class is on Wednesday.
        val wednesday = mondayClass(9 * 60, 10 * 60 + 30).copy(dayOfWeek = 3)
        assertFalse(ScheduleClash.examVsClass(at(10), 120, wednesday, zone))
    }

    @Test
    fun anExamRunningPastMidnightDoesNotClashWithEverything() {
        // 23:30 for four hours runs into the next morning. It must not be read as
        // ending "before" it starts, which would report a clash with every lecture.
        val late = mondayClass(9 * 60, 10 * 60)
        assertFalse(ScheduleClash.examVsClass(at(23, 30), 240, late, zone))
    }

    @Test
    fun aLateExamStillClashesWithSomethingItReallyCovers() {
        // 23:00 for three hours genuinely does overlap a 23:30-00:00 late class.
        val lateClass = mondayClass(23 * 60 + 30, 24 * 60)
        assertTrue(ScheduleClash.examVsClass(at(23), 180, lateClass, zone))
    }

    @Test
    fun twoExamsOverlapWhenEitherStartsInsideTheOther() {
        val nine = at(9)
        val ten = at(10)
        val eleven = at(11)

        // 09:00+2h runs 09:00-11:00 and so covers an 11:00 start at the boundary.
        assertTrue(ScheduleClash.examVsExam(nine, 120, ten, 60))
        assertFalse(ScheduleClash.examVsExam(nine, 60, ten, 60))
        assertFalse(ScheduleClash.examVsExam(nine, 120, eleven, 60))
    }

    @Test
    fun aZeroLengthExamNeverClashes() {
        // Guard against a student saving an exam with no duration by accident.
        assertFalse(ScheduleClash.examVsExam(at(9), 0, at(9), 120))
    }

    @Test
    fun backToBackIsWordedDifferentlyFromARealOverlap() {
        val lecture = mondayClass(9 * 60, 10 * 60)
        val backToBack = ScheduleClash.describe(lecture, at(10), 60, zone)
        val overlap = ScheduleClash.describe(lecture, at(9, 30), 60, zone)

        assertTrue(backToBack.detail.contains("as this class ends"))
        assertTrue(overlap.detail.contains("straight through"))
    }

    @Test
    fun clockFormattingHandlesMidnightAndOverflow() {
        assertEquals("9:30", ScheduleClash.formatClock(9 * 60 + 30))
        assertEquals("0:00", ScheduleClash.formatClock(0))
        // An overrun past midnight is reported as 24:00 rather than wrapping to 0:00,
        // which would read as "the exam ends at midnight" and hide the overrun.
        assertEquals("24:00", ScheduleClash.formatClock(24 * 60))
    }
}