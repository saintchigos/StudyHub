package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires a wake-up alarm, then re-arms it for its next matching day.
 *
 * Re-arming here is what makes the alarm repeat: the row lives in Room, and each
 * fire reads it again so editing the label or time takes effect without leaving
 * stale copies behind in AlarmManager.
 *
 * Also handles the two notification actions. Snooze re-fires after a few minutes
 * and deliberately does not touch the weekly schedule, or the student would lose
 * the rest of today's repeats.
 */
class DailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, 0L)
        if (id == 0L) return

        when (intent.action) {
            ACTION_SNOOZE -> {
                DailyAlarms.snooze(
                    context,
                    id,
                    intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty(),
                    intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true),
                    intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true)
                )
                return
            }

            ACTION_DISMISS -> {
                DailyAlarms.dismiss(context, id)
                return
            }

            ACTION -> Unit

            else -> return
        }

        val label = intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true)
        val sound = intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true)
        val snoozed = intent.getBooleanExtra(DailyAlarms.EXTRA_SNOOZED, false)

        DailyAlarms.showNotification(context, id, label, vibrate, sound)
        if (snoozed) return

        // Re-arming is one short query, and a BroadcastReceiver has no coroutine
        // scope, so it runs on a worker thread rather than the main thread.
        val pending = goAsync()
        Thread {
            try {
                val alarm = kotlinx.coroutines.runBlocking { DailyAlarms.load(context, id) }
                if (alarm != null && alarm.enabled) DailyAlarms.schedule(context, alarm)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        private const val ACTION = "com.saintchigos.studyhub.DAILY_ALARM"
        private const val ACTION_SNOOZE = DailyAlarms.ACTION_SNOOZE
        private const val ACTION_DISMISS = DailyAlarms.ACTION_DISMISS
    }
}