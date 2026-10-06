package com.saintchigos.studyhub.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One block of focused study, logged so a student can see where their time went.
 *
 * [plannedMinutes] and [actualMinutes] are both kept: students plan 25 minutes and
 * reality is what it is, and the comparison is more useful than either alone.
 */
@Entity(
    tableName = "focus_sessions",
    indices = [Index("startedAt"), Index(value = ["syncId"], unique = true)]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val courseCode: String = "",
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAt: Long,
    val finishedAt: Long,
    val completed: Boolean,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Int = 0
)

/**
 * A repeating wake-up or personal alarm, separate from class reminders.
 *
 * [daysMask] is a 7-bit value where bit 0 is Monday through bit 6 is Sunday, so a
 * weekday alarm is 0b0011111 and a weekday-and-Saturday alarm adds bit 5.
 */
@Entity(tableName = "daily_alarms")
data class DailyAlarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val minuteOfDay: Int,
    val daysMask: Int = ALL_DAYS,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val sound: Boolean = true,
    /**
     * The student's chosen tone, as a stringified content URI.
     *
     * Null means "use the phone's own alarm tone". Kept nullable so a student who
     * deletes the file they picked falls back to the system alarm rather than a
     * silent alarm.
     */
    val soundUri: String? = null
) {
    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111

        fun hasDay(mask: Int, dayOfWeek: Int): Boolean =
            dayOfWeek in 1..7 && (mask shr (dayOfWeek - 1)) and 1 == 1
    }
}

/** A row in the study log, with the course colour already resolved for the UI. */
data class FocusTotal(
    val label: String,
    val sessions: Int,
    val minutes: Int
)
