package com.saintchigos.studyhub.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * Pins the wording of the two relative-time labels.
 *
 * They are easy to mix up: [TimeUtil.relativeLabel] counts towards a deadline still
 * to come, while [TimeUtil.agoLabel] looks back at something that already happened.
 * Using the countdown wording for a reminder that just arrived made the inbox read
 * "overdue by 1 min", which is the bug these tests exist to stop coming back.
 */
class TimeUtilTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 6, 9, 0)

    private fun at(minutes: Long): Long =
        TimeUtil.toEpochMillis(now.minusMinutes(minutes))

    @Test
    fun relativeLabelCountsDownToADeadline() {
        assertEquals("now", TimeUtil.relativeLabel(at(0), now))
        assertEquals("in 5 min", TimeUtil.relativeLabel(at(-5), now))
        assertEquals("in 2 h 30 m", TimeUtil.relativeLabel(at(-150), now))
        assertEquals("in 3 days", TimeUtil.relativeLabel(at(-3 * 24 * 60), now))
        assertEquals("in 2 weeks", TimeUtil.relativeLabel(at(-14 * 24 * 60), now))
    }

    @Test
    fun relativeLabelSaysOverdueOnceADeadlineHasPassed() {
        assertEquals("overdue by 5 min", TimeUtil.relativeLabel(at(5), now))
        assertEquals("overdue by 1 day", TimeUtil.relativeLabel(at(24 * 60), now))
        assertEquals("overdue by 1 week", TimeUtil.relativeLabel(at(7 * 24 * 60), now))
    }

    @Test
    fun agoLabelLooksBackAtSomethingThatAlreadyHappened() {
        assertEquals("just now", TimeUtil.agoLabel(at(0), now))
        assertEquals("1 min ago", TimeUtil.agoLabel(at(1), now))
        assertEquals("4 min ago", TimeUtil.agoLabel(at(4), now))
        assertEquals("2 h 30 m ago", TimeUtil.agoLabel(at(150), now))
        assertEquals("3 days ago", TimeUtil.agoLabel(at(3 * 24 * 60), now))
        assertEquals("2 weeks ago", TimeUtil.agoLabel(at(14 * 24 * 60), now))
    }

    @Test
    fun agoLabelNeverCallsSomethingOverdue() {
        // A reminder filed the instant it fires must not read like a missed deadline.
        listOf(0L, 1L, 4L, 150L, 3 * 24 * 60).forEach { minutes ->
            val label = TimeUtil.agoLabel(at(minutes), now)
            assertEquals("'$label' should not say overdue", false, label.contains("overdue"))
            assertEquals("'$label' should not count down", false, label.startsWith("in "))
        }
    }

    @Test
    fun subMinuteGapsReadAsNowRatherThanZeroMinutes() {
        val thirtySecondsAgo = TimeUtil.toEpochMillis(now.minusSeconds(30))
        assertEquals("now", TimeUtil.relativeLabel(thirtySecondsAgo, now))
        assertEquals("just now", TimeUtil.agoLabel(thirtySecondsAgo, now))
    }
}