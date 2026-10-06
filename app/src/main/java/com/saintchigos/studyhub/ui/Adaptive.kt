package com.saintchigos.studyhub.ui

/**
 * Which navigation shape fits the window in front of the student.
 *
 * The same app runs on a 360dp phone and on a tablet, so the decision is made from
 * the width actually available rather than from the device model or a hardcoded
 * orientation check. A phone in landscape is wide enough to miss the rail, and a
 * tablet held upright is 601dp, which is barely over the line.
 *
 * Kept as plain functions with no Compose types so the thresholds can be tested
 * directly, which matters because the whole point is to get the boundary right.
 */
object Adaptive {

    /**
     * Where the phone layout stops working.
     *
     * 600dp is Material's own boundary between a compact phone width and anything
     * larger. It is not arbitrary: at that width a bottom bar starts pushing the
     * content into an awkward column anyway, and there is room beside it.
     */
    const val WIDE_WIDTH_DP = 600

    /**
     * Caps how wide a column of content is allowed to get.
     *
     * A tablet in landscape is around 1000dp wide. Letting a list run the full width
     * puts task titles and course codes a long way apart and makes the eye travel
     * the whole screen. Comfortable reading measure tops out well before this.
     */
    const val MAX_CONTENT_DP = 840

    /** True when the window is wide enough for a side rail rather than a bottom bar. */
    fun useRail(widthDp: Int): Boolean = widthDp >= WIDE_WIDTH_DP
}