package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Fires deadline reminders and the 07:00 daily tick.
 *
 * The tick matters more than the briefing it carries. Every schedule in the app is a
 * rolling window, so something has to run each day to push the window forward even if
 * the student never opens StudyHub. Posting the briefing is the visible half of that.
 */
class DeadlineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            DeadlineAlarms.ACTION_DEADLINE -> {
                val title = intent.getStringExtra(DeadlineAlarms.EXTRA_TITLE).orEmpty()
                if (title.isBlank()) return
                val inbox = DeadlineAlarms.post(
                    appContext,
                    intent.getStringExtra(DeadlineAlarms.EXTRA_KEY).orEmpty(),
                    title,
                    intent.getStringExtra(DeadlineAlarms.EXTRA_BODY).orEmpty(),
                    intent.getStringExtra(DeadlineAlarms.EXTRA_KIND).orEmpty()
                )
                val pending = goAsync()
                Thread {
                    try {
                        inbox?.join(3_000)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }

            DeadlineAlarms.ACTION_BRIEFING -> {
                val pending = goAsync()
                Thread {
                    try {
                        runBlocking { ReminderHub.dailyTick(appContext) }
                    } catch (t: Throwable) {
                        Log.w("StudyHubAlarms", "Daily tick failed", t)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }
}
