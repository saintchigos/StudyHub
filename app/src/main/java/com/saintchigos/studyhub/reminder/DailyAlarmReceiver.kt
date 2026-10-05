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
 */
class DailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val id = intent.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, 0L)
        val label = intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true)
        val sound = intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true)
        if (id == 0L) return

        DailyAlarms.showNotification(context, id, label, vibrate, sound)

        // Re-arming is one short query, and a BroadcastReceiver has no coroutine scope,
        // so it runs on a worker thread rather than the main thread.
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

    private companion object {
        const val ACTION = "com.saintchigos.studyhub.DAILY_ALARM"
    }
}
