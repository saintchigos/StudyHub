package com.saintchigos.studyhub.reminder

import android.content.Context
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil

/**
 * The one place that rebuilds every time-based thing from Room.
 *
 * Alarms live in AlarmManager, which forgets them on reboot, on a clock change and
 * when the app is updated, and which only ever holds the next week or two. Having one
 * function that rebuilds all of it means each trigger, a reboot, a fired reminder, the
 * daily tick or the student editing a task, repairs the whole picture the same way.
 */
object ReminderHub {

    /**
     * Re-arms class reminders, deadline reminders, the daily tick and the what's-next card.
     *
     * @param includeWakeAlarms also re-arm the wake-up alarms. Left off when a class
     * reminder fires, so a ringing or snoozed alarm is never disturbed by housekeeping.
     */
    suspend fun rearm(context: Context, includeWakeAlarms: Boolean = false) {
        val app = context.applicationContext
        val dao = StudyHubDatabase.get(app).dao()
        val sessions = dao.getAllSessions()

        ClassAlarms.reschedule(app, sessions)
        if (includeWakeAlarms) DailyAlarms.rescheduleAll(app, dao.getEnabledAlarms())
        DeadlineAlarms.reschedule(
            app,
            dao.getOpenAssignments(),
            dao.getExamsFrom(System.currentTimeMillis())
        )
        DeadlineAlarms.scheduleBriefing(app)
        NextClass.update(app, sessions)
    }

    /** The 07:00 tick: renew everything, then say good morning if there is something to say. */
    suspend fun dailyTick(context: Context) {
        val app = context.applicationContext
        // Re-arming first, so a failure while building the text cannot stop tomorrow's tick.
        rearm(app, includeWakeAlarms = true)

        if (!StudyHubPrefs(app).morningBriefing) return
        val dao = StudyHubDatabase.get(app).dao()
        val text = DeadlineAlarms.briefing(
            sessions = dao.getAllSessions(),
            assignments = dao.getOpenAssignments(),
            exams = dao.getExamsFrom(System.currentTimeMillis()),
            now = TimeUtil.now()
        ) ?: return
        DeadlineAlarms.postBriefing(app, text.first, text.second)
    }
}
