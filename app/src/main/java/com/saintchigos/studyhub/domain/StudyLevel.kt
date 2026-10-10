package com.saintchigos.studyhub.domain

import kotlin.math.sqrt

/**
 * A level and a title earned from time spent studying.
 *
 * Derived entirely from the focus log that already exists, so there is nothing new to
 * store, sync or migrate, and clearing the log resets it honestly. One minute of
 * focus is one point; every finished session adds a small bonus for showing up.
 */
object StudyLevel {

    data class Status(
        val level: Int,
        val title: String,
        val xp: Int,
        /** XP needed to reach the current level. */
        val levelStartXp: Int,
        /** XP needed to reach the next level. */
        val nextLevelXp: Int
    ) {
        /** 0.0 to 1.0 progress through the current level. */
        val progress: Float
            get() {
                val span = (nextLevelXp - levelStartXp).coerceAtLeast(1)
                return ((xp - levelStartXp).toFloat() / span).coerceIn(0f, 1f)
            }
    }

    private const val SESSION_BONUS = 5

    private val TITLES = listOf(
        "Newcomer", "Learner", "Scholar", "Achiever", "Expert", "Master", "Legend"
    )

    /** XP to reach [level]: 50 for level 2, 200 for level 3, 450 for level 4, and so on. */
    fun xpForLevel(level: Int): Int = 50 * (level - 1) * (level - 1)

    fun xp(focusMinutes: Int, sessions: Int): Int =
        focusMinutes.coerceAtLeast(0) + sessions.coerceAtLeast(0) * SESSION_BONUS

    fun status(focusMinutes: Int, sessions: Int): Status {
        val xp = xp(focusMinutes, sessions)
        // Inverse of xpForLevel, then corrected for rounding at the boundaries.
        var level = (sqrt(xp / 50.0).toInt() + 1).coerceAtLeast(1)
        while (xp < xpForLevel(level)) level--
        while (xp >= xpForLevel(level + 1)) level++
        return Status(
            level = level,
            title = TITLES[(level - 1).coerceIn(0, TITLES.size - 1)],
            xp = xp,
            levelStartXp = xpForLevel(level),
            nextLevelXp = xpForLevel(level + 1)
        )
    }
}
