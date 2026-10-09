package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Shows a class reminder, then rolls the schedule forward.
 *
 * Reminders are armed a week ahead, so a student who did not open the app for a week
 * used to stop getting any. Re-arming from here means every reminder that fires pushes
 * the window forward, and the schedule renews itself without the app ever opening.
 */
class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.saintchigos.studyhub.CLASS_REMINDER") return

        val code = intent.getStringExtra("course_code").orEmpty()
        val name = intent.getStringExtra("course_name").orEmpty()
        val room = intent.getStringExtra("room").orEmpty()
        val whenLabel = intent.getStringExtra("when_label").orEmpty()
        val lead = intent.getIntExtra("lead_minutes", 5)

        val appContext = context.applicationContext
        // The notification goes up first and synchronously: nothing below may delay it.
        val inbox = if (code.isNotBlank()) {
            ClassAlarms.showNotification(appContext, code, name, room, whenLabel, lead)
        } else {
            null
        }

        val pending = goAsync()
        Thread {
            try {
                inbox?.join(3_000)
                runBlocking { ReminderHub.rearm(appContext) }
            } catch (t: Throwable) {
                Log.w("StudyHubAlarms", "Could not roll the schedule forward", t)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
