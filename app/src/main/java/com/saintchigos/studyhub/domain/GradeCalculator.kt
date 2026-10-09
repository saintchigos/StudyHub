package com.saintchigos.studyhub.domain

/**
 * "What do I need in the exam?" arithmetic.
 *
 * Marks and weights are percentages. A course is worth `courseworkWeight` percent
 * coursework and the rest exam, so the overall mark is
 * `coursework * w + exam * (1 - w)`. Solving that for the exam gives the required mark.
 *
 * Kept free of Android so it is unit tested on the JVM. Nothing is stored: a student
 * types three numbers and reads one answer.
 */
object GradeCalculator {

    enum class Verdict {
        /** Even zero in the exam reaches the target. */
        ALREADY_THERE,

        /** A mark between 0 and 100 reaches the target. */
        POSSIBLE,

        /** The target cannot be reached, because it needs more than 100%. */
        OUT_OF_REACH
    }

    /**
     * The exam mark, in percent, needed to finish on [target].
     *
     * Returns null when the inputs cannot describe a real course: a percentage outside
     * 0 to 100, or a coursework weight of 100, which leaves no exam to solve for.
     */
    fun requiredExamMark(courseworkMark: Double, courseworkWeight: Double, target: Double): Double? {
        if (courseworkMark !in 0.0..100.0) return null
        if (courseworkWeight !in 0.0..100.0) return null
        if (target !in 0.0..100.0) return null
        val examWeight = 100.0 - courseworkWeight
        if (examWeight <= 0.0) return null
        return (target - courseworkMark * courseworkWeight / 100.0) / (examWeight / 100.0)
    }

    fun verdict(required: Double): Verdict = when {
        required <= 0.0 -> Verdict.ALREADY_THERE
        required > 100.0 -> Verdict.OUT_OF_REACH
        else -> Verdict.POSSIBLE
    }

    /** The overall mark for a given exam result, for the "what if I get 60?" question. */
    fun overall(courseworkMark: Double, courseworkWeight: Double, examMark: Double): Double =
        courseworkMark * courseworkWeight / 100.0 + examMark * (100.0 - courseworkWeight) / 100.0
}
