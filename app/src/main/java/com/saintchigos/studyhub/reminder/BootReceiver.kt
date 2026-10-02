package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saintchigos.studyhub.data.StudyHubDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot, so re-arm them once the device comes back up. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val sessions = StudyHubDatabase.get(appContext).dao().getAllSessions()
                ClassAlarms.reschedule(appContext, sessions)
            } catch (_: Exception) {
                // Nothing useful to do from a receiver; the next app launch re-arms.
            } finally {
                pending.finish()
            }
        }
    }
}