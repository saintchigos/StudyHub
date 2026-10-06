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
        // The watchdog arrives every few seconds while an alarm is still going, and
        // carries no alarm id of its own: it asks whether anything is still ringing.
        if (intent.action == ACTION_WATCHDOG) {
            val state = DailyAlarms.ringingState(context)
            // raiseScreen = false: the screen was already taken over on the first
            // ring. Re-raising it every thirty seconds wedged this OEM's
            // notification shade open and unusable.
            if (state != null) DailyAlarms.reinforce(context, state, raiseScreen = false)
            return
        }

        val id = intent.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, 0L)
        if (id == 0L) return

        val soundUri = intent.getStringExtra(DailyAlarms.EXTRA_SOUND_URI)

        when (intent.action) {
            ACTION_SNOOZE -> {
                DailyAlarms.snooze(
                    context,
                    id,
                    intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty(),
                    intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true),
                    intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true),
                    soundUri
                )
                AlarmService.stop(context)
                return
            }

            ACTION_DISMISS -> {
                DailyAlarms.dismiss(context, id)
                AlarmService.stop(context)
                return
            }

            ACTION -> Unit

            else -> return
        }

        val label = intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true)
        val sound = intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true)
        val snoozed = intent.getBooleanExtra(DailyAlarms.EXTRA_SNOOZED, false)

        // The service owns the ringing, not the notification. A notification's sound
        // belongs to the notification, so unlocking the phone dismissed it and the
        // alarm fell silent.
        //
        // markRinging + reinforce also arm the watchdog, so the alarm keeps coming
        // back if this phone reaps the service mid-sleep.
        DailyAlarms.reinforce(
            context,
            DailyAlarms.RingState(id, label, vibrate, sound, soundUri)
        )
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
        private const val ACTION_WATCHDOG = "com.saintchigos.studyhub.DAILY_ALARM_WATCHDOG"
    }
}