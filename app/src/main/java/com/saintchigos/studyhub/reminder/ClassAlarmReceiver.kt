package com.saintchigos.studyhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.saintchigos.studyhub.CLASS_REMINDER") return

        val code = intent.getStringExtra("course_code").orEmpty()
        val name = intent.getStringExtra("course_name").orEmpty()
        val room = intent.getStringExtra("room").orEmpty()
        val whenLabel = intent.getStringExtra("when_label").orEmpty()
        val lead = intent.getIntExtra("lead_minutes", 5)

        if (code.isBlank()) return
        ClassAlarms.showNotification(context, code, name, room, whenLabel, lead)
    }
}