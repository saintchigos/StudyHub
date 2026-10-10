package com.saintchigos.studyhub.domain

/**
 * A grade-point-average estimate from letter grades and credits.
 *
 * Uses the common 4.0 scale. Universities differ in the exact points per letter, so
 * the scale is a plain table that is easy to change in one place.
 */
object GpaCalculator {

    /** Letter grades in the order shown to the student, best first. */
    val GRADES: List<Pair<String, Double>> = listOf(
        "A" to 4.0,
        "A-" to 3.7,
        "B+" to 3.3,
        "B" to 3.0,
        "B-" to 2.7,
        "C+" to 2.3,
        "C" to 2.0,
        "C-" to 1.7,
        "D" to 1.0,
        "F" to 0.0
    )

    fun points(letter: String): Double? = GRADES.firstOrNull { it.first == letter }?.second

    /** One module the student has chosen a grade for. */
    data class Entry(val credits: Int, val letter: String)

    /**
     * Credit-weighted GPA, or null when nothing countable was entered.
     *
     * Entries with zero credits or an unknown letter are ignored rather than counted
     * as zero, so a half-filled list gives the GPA of what has been filled in.
     */
    fun gpa(entries: List<Entry>): Double? {
        var weighted = 0.0
        var credits = 0
        for (e in entries) {
            val p = points(e.letter) ?: continue
            if (e.credits <= 0) continue
            weighted += p * e.credits
            credits += e.credits
        }
        return if (credits == 0) null else weighted / credits
    }
}
