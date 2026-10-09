package com.saintchigos.studyhub.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saintchigos.studyhub.MainActivity
import com.saintchigos.studyhub.R
import com.saintchigos.studyhub.data.InboxLog
import com.saintchigos.studyhub.data.NotificationLog
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil

object ClassAlarms {

    /** Retired. Its sound and vibration were frozen at creation, so the settings did nothing. */
    private const val LEGACY_CHANNEL_ID = "class_reminders_v1"

    /**
     * One channel per sound/vibration combination.
     *
     * From Android 8 the channel, not the notification, decides the sound and the
     * vibration, and a channel cannot be changed after it is created. So the Alert sound
     * and Vibration switches in Settings only work if each combination is its own
     * channel and the right one is picked when the reminder is posted.
     */
    fun channelId(sound: Boolean, vibrate: Boolean): String =
        "class_reminders_v2_s${if (sound) 1 else 0}_v${if (vibrate) 1 else 0}"
    private const val PREFS = "class_alarms"
    private const val KEYS = "scheduled_keys"

    private const val EXTRA_SESSION_ID = "session_id"
    private const val EXTRA_CODE = "course_code"
    private const val EXTRA_NAME = "course_name"
    private const val EXTRA_ROOM = "room"
    private const val EXTRA_WHEN = "when_label"
    private const val EXTRA_LEAD = "lead_minutes"
    private const val EXTRA_KEY = "reminder_key"

    /** Loud, heads-up channel so the reminder is impossible to miss. */
    fun ensureChannel(context: Context) {
        ensureChannel(context, StudyHubPrefs(context).reminderSound, StudyHubPrefs(context).reminderVibrate)
    }

    private fun ensureChannel(context: Context, sound: Boolean, vibrate: Boolean): String {
        val id = channelId(sound, vibrate)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return id
        // The old channel would otherwise sit in system settings next to the new ones.
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(id) != null) return id

        val channel = NotificationChannel(
            id,
            "Class reminders" + when {
                sound && vibrate -> ""
                sound -> " (sound only)"
                vibrate -> " (vibrate only)"
                else -> " (silent)"
            },
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warns you before a class starts"
            enableVibration(vibrate)
            if (vibrate) vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            if (sound) {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
            } else {
                setSound(null, null)
            }
        }
        manager.createNotificationChannel(channel)
        return id
    }

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(context: Context, r: ClassReminder): PendingIntent {
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = "com.saintchigos.studyhub.CLASS_REMINDER"
            putExtra(EXTRA_SESSION_ID, r.sessionId)
            putExtra(EXTRA_CODE, r.session.courseCode)
            putExtra(EXTRA_NAME, r.session.courseName)
            putExtra(EXTRA_ROOM, r.session.room)
            putExtra(EXTRA_WHEN, r.whenLabel)
            putExtra(EXTRA_LEAD, r.leadMinutes)
            putExtra(EXTRA_KEY, r.key)
        }
        return PendingIntent.getBroadcast(
            context,
            r.key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancel(context: Context, key: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = "com.saintchigos.studyhub.CLASS_REMINDER"
        }
        val pi = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Cancels every pending reminder and re-arms from the current timetable. */
    fun reschedule(context: Context, sessions: List<SessionWithCourse>) {
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldKeys = prefs.getStringSet(KEYS, emptySet()) ?: emptySet()

        val now = TimeUtil.now()
        val settings = StudyHubPrefs(context)
        val reminders = ReminderRules.computeReminders(
            sessions = sessions,
            now = now,
            firstLead = settings.firstClassLeadMinutes,
            laterLead = settings.otherClassLeadMinutes
        )
        val newKeys = reminders.map { it.key }.toSet()
        android.util.Log.i(
            "StudyHubAlarms",
            "reschedule sessions=${sessions.size} reminders=${reminders.size} " +
                "now=$now exact=${canScheduleExact(context)}"
        )

        // Cancel stored keys plus anything in the key space we could have armed before,
        // so alarms for deleted or retimed classes never survive a reschedule.
        val sweep = ReminderRules.candidateKeys(sessions, now) + oldKeys
        for (key in sweep) {
            if (!newKeys.contains(key)) cancel(context, key)
        }

        val exact = canScheduleExact(context)
        for (r in reminders) {
            val pi = pendingIntent(context, r)
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.triggerAtMillis, pi)
            } else {
                // Degrade to an inexact alarm rather than dropping reminders entirely.
                // The while-idle variant, because a plain window is deferred by Doze.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.triggerAtMillis, pi)
            }
        }

        prefs.edit().putStringSet(KEYS, newKeys).apply()
    }

    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keys = prefs.getStringSet(KEYS, emptySet()) ?: emptySet()
        for (key in keys) cancel(context, key)
        prefs.edit().putStringSet(KEYS, emptySet()).apply()
    }

    fun showNotification(
        context: Context,
        courseCode: String,
        courseName: String,
        room: String,
        whenLabel: String,
        leadMinutes: Int
    ): Thread? {
        val settings = StudyHubPrefs(context)
        val channelId = ensureChannel(context, settings.reminderSound, settings.reminderVibrate)

        val title = if (leadMinutes > 0)
            "$courseCode starts in $leadMinutes min"
        else
            "$courseCode has started"

        val body = buildString {
            append(courseName)
            if (room.isNotBlank()) {
                append("  ·  ")
                append(room)
            }
            append("  ·  ")
            append(whenLabel)
            if (leadMinutes > 0) append(" start")
        }

        // Filed in the in-app inbox before the notification is even attempted, so a
        // reminder still reaches the student when the system has notifications
        // blocked, and when they swiped the shade entry away without reading it.
        // The point of the inbox is catching the ones that never got through.
        val inbox = InboxLog.record(context, NotificationLog.KIND_CLASS, title, body)

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return inbox

        val open = PendingIntent.getActivity(
            context,
            1001,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setDefaults(0)

        if (settings.reminderSound) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }
        if (settings.reminderVibrate) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
        }

        val notification = builder.build()

        try {
            manager.notify((courseCode + whenLabel).hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; the in-app inbox already has it.
        }
        return inbox
    }

    /** Posts a sample reminder so a student can confirm alerts reach them. */
    fun showTestNotification(context: Context) {
        showNotification(
            context = context,
            courseCode = "TEST",
            courseName = "This is how your class alerts will look",
            room = "Reminder test",
            whenLabel = "Reminders are working",
            leadMinutes = 5
        )
    }
}