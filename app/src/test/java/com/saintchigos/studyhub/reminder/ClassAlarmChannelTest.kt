package com.saintchigos.studyhub.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Android fixes a notification channel's sound and vibration when it is created, so the
 * Alert sound and Vibration switches only work if every combination has its own channel.
 */
class ClassAlarmChannelTest {

    @Test
    fun everySoundAndVibrationCombinationHasItsOwnChannel() {
        val ids = listOf(
            ClassAlarms.channelId(sound = true, vibrate = true),
            ClassAlarms.channelId(sound = true, vibrate = false),
            ClassAlarms.channelId(sound = false, vibrate = true),
            ClassAlarms.channelId(sound = false, vibrate = false)
        )
        assertEquals(4, ids.toSet().size)
    }

    @Test
    fun theSameSettingsAlwaysGiveTheSameChannel() {
        assertEquals(
            ClassAlarms.channelId(sound = true, vibrate = false),
            ClassAlarms.channelId(sound = true, vibrate = false)
        )
    }
}
