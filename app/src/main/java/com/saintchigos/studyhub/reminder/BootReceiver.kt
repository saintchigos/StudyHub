package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saintchigos.studyhub.data.StudyHubDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Brings everything time-based back after a reboot or an app update.
 *
 * Alarms and exact alarm intents do not survive either, and posted notifications are
 * dropped by the system, so all three have to be re-established from Room here.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = StudyHubDatabase.get(appContext).dao()
                val sessions = dao.getAllSessions()
                ClassAlarms.reschedule(appContext, sessions)
                // Wake-up alarms die with the reboot too, so they are re-armed from
                // Room rather than from any stored copy in AlarmManager.
                DailyAlarms.rescheduleAll(appContext, dao.getEnabledAlarms())
                // The what's-next card is a posted notification, and the system drops
                // those across a reboot, so it is reposted here too. update() honours
                // the student's switch itself, so this cannot post after they turn it
                // off, and it clears the card when the timetable is empty rather than
                // leaving yesterday's class on the lock screen.
                NextClass.update(appContext, sessions)
            } catch (_: Exception) {
                // Nothing useful to do from a receiver; the next app launch re-arms.
            } finally {
                pending.finish()
            }
        }
    }
}