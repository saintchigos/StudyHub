package com.saintchigos.studyhub.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rail boundary is the kind of number that looks obvious and is easy to get
 * wrong, so it is pinned here rather than left to a manual glance on one device.
 */
class AdaptiveTest {

    @Test
    fun aNarrowPhoneKeepsTheBottomBar() {
        // The common Android phone.
        assertFalse(Adaptive.useRail(360))
    }

    @Test
    fun theWidestPhonesStillKeepTheBottomBar() {
        // Big phones reach the low 400s; none of them get a rail.
        assertFalse(Adaptive.useRail(412))
        assertFalse(Adaptive.useRail(431))
        assertFalse(Adaptive.useRail(480))
    }

    @Test
    fun aPhoneInLandscapeGetsTheRail() {
        // 360 wide becomes 800 tall once turned, and a bottom bar down the side of a
        // landscape phone wastes the width the student just turned the screen to get.
        assertTrue(Adaptive.useRail(800))
    }

    @Test
    fun aSmallTabletHeldUprightJustClearsTheBar() {
        // Measured on a Galaxy Tab A7 Lite: 800px at 213dpi is 601dp portrait, so this
        // is the real device sitting one dp above the boundary.
        assertTrue(Adaptive.useRail(601))
    }

    @Test
    fun theBoundaryItselfGoesToTheRail() {
        assertTrue(Adaptive.useRail(Adaptive.WIDE_WIDTH_DP))
        assertFalse(Adaptive.useRail(Adaptive.WIDE_WIDTH_DP - 1))
    }

    @Test
    fun aLandscapeTabletIsWellPastTheBoundary() {
        assertTrue(Adaptive.useRail(1007))
    }

    @Test
    fun contentIsCappedSoLinesStayReadable() {
        // The cap has to actually bite on a landscape tablet, or it is dead code.
        assertTrue(Adaptive.MAX_CONTENT_DP < 1007)
        // And must never constrain a phone.
        assertTrue(Adaptive.MAX_CONTENT_DP > 480)
    }
}