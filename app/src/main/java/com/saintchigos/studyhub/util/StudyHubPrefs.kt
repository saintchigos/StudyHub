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

    /**
     * Day of the last class alert the student acted on, as an epoch day number.
     * Used to show a study streak. Stored as a long so no new table is needed.
     */
    var lastActiveDay: Long
        get() = prefs.getLong(KEY_LAST_ACTIVE_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ACTIVE_DAY, value).apply()

    /** Length of the current streak, kept alongside [lastActiveDay]. */
    var streakDays: Int
        get() = prefs.getInt(KEY_STREAK_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK_DAYS, value).apply()

    /** Whether the one-time welcome tour has been dismissed. */
    var seenWelcome: Boolean
        get() = prefs.getBoolean(KEY_SEEN_WELCOME, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN_WELCOME, value).apply()

    /** Whether the "what's next" study tip card is showing on Home. */
    var showStudyTips: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIPS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIPS, value).apply()

    /** "system", "light" or "dark". */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    /**
     * Which accent palette to generate the theme from, as an [com.saintchigos.studyhub.ui.theme.Acccent] name.
     *
     * Stored by name rather than as a colour so the scheme can be regenerated from a
     * seed later and still look right, and so an accent added in a future release
     * cannot break an existing install.
     */
    var accent: String
        get() = prefs.getString(KEY_ACCENT, "OCEAN") ?: "OCEAN"
        set(value) = prefs.edit().putString(KEY_ACCENT, value).apply()

    /** Whether the wallpaper's colours should win over the chosen accent. */
    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    /** True black backgrounds in dark mode, which saves power on OLED screens. */
    var amoled: Boolean
        get() = prefs.getBoolean(KEY_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED, value).apply()

    /**
     * Text size multiplier applied on top of the phone's own font size.
     *
     * A student who finds the default too small should not have to leave the app to
     * change it, and the phone setting may already be turned down for battery.
     */
    var fontScale: Float
        get() = prefs.getFloat(KEY_FONT_SCALE, 1f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SCALE, value.coerceIn(0.85f, 1.5f)).apply()

    private companion object {
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_FIRST_LEAD = "first_class_lead_minutes"
        const val KEY_OTHER_LEAD = "other_class_lead_minutes"
        const val KEY_SOUND = "reminder_sound"
        const val KEY_VIBRATE = "reminder_vibrate"
        const val KEY_TERMS = "terms_accepted"
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_AMOLED = "amoled"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_LAST_ACTIVE_DAY = "last_active_day"
        const val KEY_STREAK_DAYS = "streak_days"
        const val KEY_SEEN_WELCOME = "seen_welcome"
        const val KEY_SHOW_TIPS = "show_study_tips"
    }
}