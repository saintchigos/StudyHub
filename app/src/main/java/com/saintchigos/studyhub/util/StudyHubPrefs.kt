package com.saintchigos.studyhub.util

import android.content.Context

/** User adjustable app settings plus onboarding state. */
class StudyHubPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("studyhub_prefs", Context.MODE_PRIVATE)

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    /** How long before the first class of the day the reminder fires. */
    var firstClassLeadMinutes: Int
        get() = prefs.getInt(KEY_FIRST_LEAD, 10)
        set(value) = prefs.edit().putInt(KEY_FIRST_LEAD, value.coerceIn(1, 120)).apply()

    /** How long before every other class the reminder fires. */
    var otherClassLeadMinutes: Int
        get() = prefs.getInt(KEY_OTHER_LEAD, 5)
        set(value) = prefs.edit().putInt(KEY_OTHER_LEAD, value.coerceIn(1, 120)).apply()

    var reminderSound: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var reminderVibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var termsAccepted: Boolean
        get() = prefs.getBoolean(KEY_TERMS, false)
        set(value) = prefs.edit().putBoolean(KEY_TERMS, value).apply()

    /** "system", "light" or "dark". */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    private companion object {
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_FIRST_LEAD = "first_class_lead_minutes"
        const val KEY_OTHER_LEAD = "other_class_lead_minutes"
        const val KEY_SOUND = "reminder_sound"
        const val KEY_VIBRATE = "reminder_vibrate"
        const val KEY_TERMS = "terms_accepted"
        const val KEY_THEME = "theme_mode"
    }
}