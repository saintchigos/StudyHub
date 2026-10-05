package com.saintchigos.studyhub.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saintchigos.studyhub.MainActivity
import com.saintchigos.studyhub.R

/**
 * Notifications that belong to the focus timer rather than the timetable.
 *
 * Kept separate from [com.saintchigos.studyhub.reminder.ClassAlarms] so a student
 * can silence class warnings without losing their own alarms, and turn study
 * nudges off without touching either.
 */
object StudyHubNotifications {

    const val FOCUS_CHANNEL_ID = "focus_v1"

    fun ensureFocusChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(FOCUS_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                FOCUS_CHANNEL_ID,
                "Focus sessions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tells you when a focus block starts and finishes"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun showStudyStarted(context: Context, label: String, minutes: Int) {
        ensureFocusChannel(context)
        post(
            context = context,
            id = FOCUS_START_ID,
            title = "$minutes minutes of $label",
            body = "Timer is running. Put the phone down and get to work."
        )
    }

    fun showStudyFinished(context: Context, label: String, minutes: Int) {
        ensureFocusChannel(context)
        post(
            context = context,
            id = FOCUS_FINISHED_ID,
            title = "Block finished",
            body = "$minutes minutes of $label done. Take a short break."
        )
    }

    private const val FOCUS_START_ID = 2001
    private const val FOCUS_FINISHED_ID = 2002

    private fun post(context: Context, id: Int, title: String, body: String) {
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; the in-app timer still counts down.
        }
    }
}
