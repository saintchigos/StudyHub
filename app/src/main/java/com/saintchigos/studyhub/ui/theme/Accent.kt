package com.saintchigos.studyhub.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The accent colours a student can pick from.
 *
 * Each entry is only a *seed*: the whole light and dark scheme is generated from it,
 * so picking one cannot produce a screen where the primary is readable on the
 * background, or where the error colour clashes with the accent. Hand written pairs
 * of primary/onPrimary are the usual way themes end up unreadable in dark mode.
 *
 * The seeds are spaced far enough apart in hue that the generated palettes read as
 * genuinely different apps rather than as slightly different blues.
 */
enum class Accent(
    val label: String,
    val seed: Color,
) {
    /** The StudyHub default: a calm blue that stays legible in bright sunlight. */
    OCEAN("Ocean", Color(0xFF1B5E9E)),

    /** Green reads as "done", which suits a study app people open to tick things off. */
    FOREST("Forest", Color(0xFF2E6B3F)),

    /** Violet, for students who want the app to feel less like a utility. */
    VIOLET("Violet", Color(0xFF6A3D9A)),

    /** Warm orange, the friendliest of the set for evening study sessions. */
    SUNSET("Sunset", Color(0xFFA8430F)),

    /** Deep magenta, high contrast and easy to tell apart from every other option. */
    BERRY("Berry", Color(0xFFA3003B)),

    /** Near neutral slate. Lowest chroma, so it disturbs the least content. */
    GRAPHITE("Graphite", Color(0xFF3F5F7A));

    companion object {
        val DEFAULT = OCEAN

        /** Looks up a stored accent, falling back to the default if it no longer exists. */
        fun fromName(name: String?): Accent =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}