package com.saintchigos.studyhub.reminder

import com.saintchigos.studyhub.data.DailyAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Checks that repeating alarms land on the days the student actually ticked.
 *
 * The bug this guards against is invisible in the UI: `java.util.Calendar` counts
 * Sunday as day 1, while `DailyAlarm.daysMask` treats bit 0 as Monday. Feeding
 * Calendar's number straight into the mask shifts every alarm by one day, so a
 * weekday alarm rings Sunday to Thursday and never on Friday.
 */
class DailyAlarmScheduleTest {

    /** 2024-10-07 is a Monday. Built from parts so the test never depends on the default zone. */
    private fun dateIn(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun weekdayAlarmTodayFiresToday() {
        // Monday 8:00am, and it is already 7:00am on Monday: today, not next week.
        val now = dateIn(2024, Calendar.OCTOBER, 7, 7, 0)
        val alarm = DailyAlarm(label = "Wake up", minuteOfDay = 8 * 60, daysMask = DailyAlarm.WEEKDAYS)

        val trigger = at(alarm, now)
        assertEquals(Monday, dayOfWeek(trigger))
        assertEquals(8 * 60, minuteOfDay(trigger))
    }

    @Test
    fun weekdayAlarmAfterTimeRollsToTheNextWeekdayNotSunday() {
        // Monday 9:00am asking for an 8:00am weekday alarm: next chance is Tuesday,
        // not Sunday. Getting Sunday here is exactly the off-by-one bug.
        val now = dateIn(2024, Calendar.OCTOBER, 7, 9, 0)
        val alarm = DailyAlarm(label = "Wake up", minuteOfDay = 8 * 60, daysMask = DailyAlarm.WEEKDAYS)

        val trigger = at(alarm, now)
        assertEquals(Tuesday, dayOfWeek(trigger))
        assertEquals(8, Calendar.getInstance().apply { timeInMillis = trigger }.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun weekendAlarmSkipsToSaturdayNotFriday() {
        // Friday evening asking for a 7:00am Saturday alarm must not fire on Friday.
        val now = dateIn(2024, Calendar.OCTOBER, 11, 20, 0)
        val alarm = DailyAlarm(
            label = "Library",
            minuteOfDay = 7 * 60,
            daysMask = 1 shl 5
        )

        val trigger = at(alarm, now)
        assertEquals(Saturday, dayOfWeek(trigger))
        assertEquals(12, Calendar.getInstance().apply { timeInMillis = trigger }.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun sundayAlarmIsReachable() {
        // Sunday is bit 6 in the mask. Calendar counts it as 1, so this is the case
        // that would have fired on Monday if the numbering was not converted.
        val now = dateIn(2024, Calendar.OCTOBER, 8, 9, 0) // Tuesday
        val alarm = DailyAlarm(label = "Church", minuteOfDay = 9 * 60, daysMask = 1 shl 6)

        val trigger = at(alarm, now)
        assertEquals(Sunday, dayOfWeek(trigger))
        assertEquals(13, Calendar.getInstance().apply { timeInMillis = trigger }.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun dailyAlarmNeverReturnsTodayAfterItHasPassed() {
        val now = dateIn(2024, Calendar.OCTOBER, 7, 23, 30)
        val alarm = DailyAlarm(label = "Sleep", minuteOfDay = 22 * 60, daysMask = DailyAlarm.ALL_DAYS)

        val trigger = at(alarm, now)
        assertEquals(Tuesday, dayOfWeek(trigger))
        assertEquals(22 * 60, minuteOfDay(trigger))
        assertEquals(true, trigger > now)
    }

    @Test
    fun exactTimeIsNeverInThePast() {
        // Exactly on the minute: must move forward, otherwise it fires instantly.
        val now = dateIn(2024, Calendar.OCTOBER, 7, 8, 0)
        val alarm = DailyAlarm(label = "Wake up", minuteOfDay = 8 * 60, daysMask = DailyAlarm.ALL_DAYS)

        assertEquals(true, at(alarm, now) > now)
    }

    @Test
    fun anAlarmWithNoDaysSelectedIsRejected() {
        // Bit mask 0 means no days at all. Saving that would create an alarm which
        // looks enabled in the list but can never ring again.
        val now = dateIn(2024, Calendar.OCTOBER, 7, 7, 0)
        val alarm = DailyAlarm(label = "Never", minuteOfDay = 8 * 60, daysMask = 0)

        assertNull(DailyAlarms.nextTriggerAt(alarm, now))
    }

    @Test
    fun snoozeDelayIsNineMinutes() {
        assertEquals(9, DailyAlarms.SNOOZE_MINUTES)
    }

    @Test
    fun formatTimeUsesTwelveHourClock() {
        assertEquals("6:30 AM", DailyAlarms.formatTime(6 * 60 + 30))
        assertEquals("12:00 AM", DailyAlarms.formatTime(0))
        assertEquals("12:15 PM", DailyAlarms.formatTime(12 * 60 + 15))
        assertEquals("11:59 PM", DailyAlarms.formatTime(23 * 60 + 59))
    }

    @Test
    fun daySummaryReadsPlainly() {
        assertEquals("Every day", DailyAlarms.daySummary(DailyAlarm.ALL_DAYS))
        assertEquals("Weekdays", DailyAlarms.daySummary(DailyAlarm.WEEKDAYS))
        // Bit 0 is Monday and bit 6 is Sunday, so this mask is Monday and Sunday.
        assertEquals("Mon Sun", DailyAlarms.daySummary(0b1000001))
        assertEquals("Wed", DailyAlarms.daySummary(1 shl 2))
    }

    /** Fails loudly if an alarm that should fire resolves to no trigger at all. */
    private fun at(alarm: DailyAlarm, now: Long): Long =
        requireNotNull(DailyAlarms.nextTriggerAt(alarm, now)) {
            "expected ${alarm.label} to have a next trigger"
        }

    private fun dayOfWeek(millis: Long): Int {
        val calendarDay = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
        return if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
    }

    private fun minuteOfDay(millis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private companion object {
        const val Monday = 1
        const val Tuesday = 2
        const val Saturday = 6
        const val Sunday = 7
    }
}