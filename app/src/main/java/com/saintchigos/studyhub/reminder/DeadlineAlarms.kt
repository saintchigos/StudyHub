package com.saintchigos.studyhub.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saintchigos.studyhub.MainActivity
import com.saintchigos.studyhub.R
import com.saintchigos.studyhub.data.AssignmentWithCourse
import com.saintchigos.studyhub.data.ExamWithCourse
import com.saintchigos.studyhub.data.InboxLog
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDateTime

/**
 * Reminders for things that are due, and a short briefing each morning.
 *
 * Until now only classes could remind a student of anything, so an assignment due at
 * midnight or an exam on Monday relied on them opening the app. This warns ahead of
 * each deadline and files every warning in the same in-app inbox as class reminders.
 *
 * Everything is derived from Room each time it is armed, so nothing here is a second
 * copy of the data that could drift out of step with it.
 */
object DeadlineAlarms {

    const val CHANNEL_ID = "deadline_reminders_v1"
    const val ACTION_DEADLINE = "com.saintchigos.studyhub.DEADLINE_REMINDER"
    const val ACTION_BRIEFING = "com.saintchigos.studyhub.MORNING_BRIEFING"

    const val EXTRA_KEY = "deadline_key"
    const val EXTRA_TITLE = "deadline_title"
    const val EXTRA_BODY = "deadline_body"
    const val EXTRA_KIND = "deadline_kind"

    private const val PREFS = "deadline_alarms"
    private const val KEYS = "scheduled_keys"
    private const val REQUEST_SALT = 0x5D000000
    private const val BRIEFING_REQUEST = 0x70
    private const val BRIEFING_NOTIFICATION_ID = 0x4252

    /** The briefing goes out at 07:00. It is also the daily tick that renews every schedule. */
    const val BRIEFING_MINUTE_OF_DAY = 7 * 60

    fun plan(
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: Long
    ): List<DeadlinePlanner.Reminder> = DeadlinePlanner.plan(assignments, exams, now)

    fun briefing(
        sessions: List<SessionWithCourse>,
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: LocalDateTime
    ): Pair<String, String>? = DeadlinePlanner.briefing(sessions, assignments, exams, now)

    // ---- notifications ------------------------------------------------------

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Deadlines and daily briefing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warns you before tasks and exams are due, and summarises your day"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Posts one deadline reminder and files it in the inbox. Returns the inbox write. */
    fun post(context: Context, key: String, title: String, body: String, kind: String): Thread? {
        ensureChannel(context)
        val inbox = InboxLog.record(context, kind, title, body)
        notify(context, key.hashCode(), title, body)
        return inbox
    }

    fun postBriefing(context: Context, title: String, body: String) {
        ensureChannel(context)
        notify(context, BRIEFING_NOTIFICATION_ID, title, body)
    }

    private fun notify(context: Context, id: Int, title: String, body: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; the inbox already has it.
        }
    }

    // ---- scheduling ---------------------------------------------------------

    private fun requestCode(key: String): Int = key.hashCode() xor REQUEST_SALT

    private fun setWake(context: Context, am: AlarmManager, at: Long, pi: PendingIntent) {
        val exact = ClassAlarms.canScheduleExact(context)
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancel(context: Context, key: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(key),
            Intent(context, DeadlineAlarmReceiver::class.java).apply { action = ACTION_DEADLINE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /**
     * Cancels what is no longer wanted and arms what is.
     *
     * Honours the student's switch here, so there is no way to arm a reminder they
     * turned off. Called after every edit and by the daily tick.
     */
    fun reschedule(
        context: Context,
        assignments: List<AssignmentWithCourse>,
        exams: List<ExamWithCourse>,
        now: Long = System.currentTimeMillis()
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldKeys = prefs.getStringSet(KEYS, emptySet()) ?: emptySet()

        val wanted = if (StudyHubPrefs(context).deadlineReminders) {
            plan(assignments, exams, now)
        } else {
            emptyList()
        }
        val newKeys = wanted.map { it.key }.toSet()

        for (key in oldKeys) {
            if (key !in newKeys) cancel(context, key)
        }
        for (r in wanted) {
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode(r.key),
                Intent(context, DeadlineAlarmReceiver::class.java).apply {
                    action = ACTION_DEADLINE
                    putExtra(EXTRA_KEY, r.key)
                    putExtra(EXTRA_TITLE, r.title)
                    putExtra(EXTRA_BODY, r.body)
                    putExtra(EXTRA_KIND, r.kind)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setWake(context, am, r.triggerAt, pi)
        }
        prefs.edit().putStringSet(KEYS, newKeys).apply()
    }

    /** Next 07:00, today if it has not passed. Always armed: it is the tick that renews everything. */
    fun scheduleBriefing(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = TimeUtil.now()
        var next = now.toLocalDate().atTime(TimeUtil.minuteOfDayToLocalTime(BRIEFING_MINUTE_OF_DAY))
        if (!next.isAfter(now)) next = next.plusDays(1)

        val pi = PendingIntent.getBroadcast(
            context,
            BRIEFING_REQUEST,
            Intent(context, DeadlineAlarmReceiver::class.java).apply { action = ACTION_BRIEFING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setWake(context, am, TimeUtil.toEpochMillis(next), pi)
    }
}
