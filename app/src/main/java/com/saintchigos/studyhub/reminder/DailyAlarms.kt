package com.saintchigos.studyhub.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saintchigos.studyhub.R
import com.saintchigos.studyhub.data.DailyAlarm
import com.saintchigos.studyhub.data.StudyHubDatabase
import java.util.Calendar

/**
 * Repeating wake-up alarms, separate from class reminders.
 *
 * How this works: each alarm gets one `AlarmManager` trigger at its next real
 * occurrence. When it fires, [DailyAlarmReceiver] posts the alarm and schedules
 * the same alarm again for the next matching weekday. Re-arming on fire keeps
 * a repeating alarm correct across days a phone is switched off, and sidesteps
 * the weekly-repetition limits of `setRepeating`, which drifts.
 */
object DailyAlarms {

    const val CHANNEL_ID = "daily_alarms_v1"
    private const val PREFS = "daily_alarms"
    private const val KEYS = "scheduled_keys"

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_LABEL = "label"
    const val EXTRA_VIBRATE = "vibrate"
const val EXTRA_SOUND = "sound"
const val EXTRA_SOUND_URI = "sound_uri"
internal const val EXTRA_SNOOZED = "snoozed"
const val SNOOZE_MINUTES = 9

/**
 * The label used when a student has not named their alarm.
 *
 * A wake-up alarm saying nothing is easy to sleep through, so the default is
 * personal rather than generic.
 */
const val DEFAULT_ALARM_LABEL = "Wake up Mr Chigos"

    private const val ACTION = "com.saintchigos.studyhub.DAILY_ALARM"
    const val ACTION_SNOOZE = "com.saintchigos.studyhub.DAILY_ALARM_SNOOZE"
    const val ACTION_DISMISS = "com.saintchigos.studyhub.DAILY_ALARM_DISMISS"

    private const val SNOOZE_REQUEST = 0x50
    private const val DISMISS_REQUEST = 0x51
    private const val RING_REQUEST = 0x52

    /** Shows the alarm again in [SNOOZE_MINUTES] minutes without touching its schedule. */
    fun snooze(
        context: Context,
        alarmId: Long,
        label: String,
        vibrate: Boolean,
        sound: Boolean,
        soundUri: String? = null
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, DailyAlarmReceiver::class.java).apply {
            action = ACTION
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_SOUND, sound)
            putExtra(EXTRA_SOUND_URI, soundUri)
            // Marks this as a snooze so the receiver re-arms by snooze delay rather
            // than by the next weekday, which would skip today's alarm entirely.
            putExtra(EXTRA_SNOOZED, true)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun dismiss(context: Context, alarmId: Long) {
        NotificationManagerCompat.from(context).cancel(alarmId.hashCode())
    }

    // ---- Surviving a phone that kills the ringing service ---------------------
    //
    // A foreground service is not a guarantee on a cheap phone. Budget
    // Transsion/Android builds routinely reap one within seconds of it starting,
    // and the alarm goes silent while the student is still asleep.
    //
    // The fix is not to trust the service but to keep re-arming it. While an alarm
    // is ringing, a short exact alarm fires a watchdog that starts the service
    // again. Exact alarms are exempt from Doze and aggressive app reaping, so this
    // keeps ringing until the student actually presses a button. Pressing a button
    // clears the saved state, so the loop ends itself - it is not a runaway.

    private const val RING_PREFS = "daily_alarm_ringing"
    private const val KEY_RINGING = "ringing"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_LABEL = "label"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_SOUND = "sound"
    private const val KEY_SOUND_URI = "sound_uri"

    private const val ACTION_WATCHDOG = "com.saintchigos.studyhub.DAILY_ALARM_WATCHDOG"
    private const val WATCHDOG_REQUEST = 0x53

    /** How often the ringing alarm re-arms itself while still unanswered. */
    private const val WATCHDOG_INTERVAL_MS = 30_000L

    /**
     * How long an alarm keeps ringing before giving up.
     *
     * Long enough to wake someone who slept through the first ten minutes, short
     * enough that an alarm left running in a pocket does not shout all morning.
     */
    private const val RING_MAX_MS = 15 * 60_000L

    /** What the watchdog needs to put the same alarm back. */
    data class RingState(
        val alarmId: Long,
        val label: String,
        val vibrate: Boolean,
        val sound: Boolean,
        val soundUri: String?
    )

    fun markRinging(
        context: Context,
        alarmId: Long,
        label: String,
        vibrate: Boolean,
        sound: Boolean,
        soundUri: String?
    ) {
        val prefs = context.getSharedPreferences(RING_PREFS, Context.MODE_PRIVATE)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L).takeIf { prefs.getBoolean(KEY_RINGING, false) }
            ?: System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_RINGING, true)
            .putLong(KEY_STARTED_AT, startedAt)
            .putLong(KEY_ALARM_ID, alarmId)
            .putString(KEY_LABEL, label)
            .putBoolean(KEY_VIBRATE, vibrate)
            .putBoolean(KEY_SOUND, sound)
            .putString(KEY_SOUND_URI, soundUri)
            .apply()
    }

    fun ringingState(context: Context): RingState? {
        val prefs = context.getSharedPreferences(RING_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_RINGING, false)) return null

        // An alarm that has already shouted for [RING_MAX_MS] has given itself up,
        // so the student gets a missed alarm rather than a permanently ringing phone.
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        if (startedAt == 0L || System.currentTimeMillis() - startedAt > RING_MAX_MS) {
            // Clearing the state is not enough: the ringing service has to be
            // stopped too, or the alarm keeps shouting with nothing left tracking it.
            AlarmService.stop(context)
            return null
        }
        return RingState(
            alarmId = prefs.getLong(KEY_ALARM_ID, 0L),
            label = prefs.getString(KEY_LABEL, DEFAULT_ALARM_LABEL).orEmpty(),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
            sound = prefs.getBoolean(KEY_SOUND, true),
            soundUri = prefs.getString(KEY_SOUND_URI, null)
        )
    }

    /**
     * Stops the alarm for good.
     *
     * Called by both "Turn off" and "Snooze", so this is the single place that ends
     * the watchdog loop. Nothing else may clear it, or the alarm could fall silent
     * on its own.
     */
    fun clearRinging(context: Context) {
        context.getSharedPreferences(RING_PREFS, Context.MODE_PRIVATE).edit()
            .clear()
            .apply()
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            WATCHDOG_REQUEST,
            Intent(context, DailyAlarmReceiver::class.java).apply { action = ACTION_WATCHDOG },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Restarts the ringing alarm and lines up the next watchdog. */
    fun reinforce(context: Context, state: RingState, raiseScreen: Boolean = true) {
        markRinging(
            context, state.alarmId, state.label, state.vibrate, state.sound, state.soundUri
        )
        AlarmService.start(
            context, state.alarmId, state.label, state.vibrate, state.sound, state.soundUri,
            raiseScreen = raiseScreen
        )
        scheduleWatchdog(context)
    }

    private fun scheduleWatchdog(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            WATCHDOG_REQUEST,
            Intent(context, DailyAlarmReceiver::class.java).apply { action = ACTION_WATCHDOG },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + WATCHDOG_INTERVAL_MS
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** Reads one alarm back out of Room, so a fired alarm can re-arm itself. */
    suspend fun load(context: Context, id: Long): DailyAlarm? =
        StudyHubDatabase.get(context).dao().getAlarm(id)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Wake-up alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Your own wake-up and study alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(context: Context, alarm: DailyAlarm): PendingIntent {
        val intent = Intent(context, DailyAlarmReceiver::class.java).apply {
            action = ACTION
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_LABEL, alarm.label)
            putExtra(EXTRA_VIBRATE, alarm.vibrate)
            putExtra(EXTRA_SOUND, alarm.sound)
            putExtra(EXTRA_SOUND_URI, alarm.soundUri)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Next time this alarm should ring, as a wall-clock instant in millis.
     *
     * Returns null when the alarm has no days ticked. Such an alarm can never
     * fire, so callers must not schedule it instead of quietly landing on some
     * arbitrary day a week out.
     */
    fun nextTriggerAt(alarm: DailyAlarm, now: Long = System.currentTimeMillis()): Long? {
        if (alarm.daysMask == 0) return null

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.HOUR_OF_DAY, alarm.minuteOfDay / 60)
        calendar.set(Calendar.MINUTE, alarm.minuteOfDay % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)

        // Walk forward at most a week looking for a day this alarm is set on.
        var attempts = 0
        while (!DailyAlarm.hasDay(alarm.daysMask, isoDayOfWeek(calendar)) && attempts < 7) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            attempts++
        }
        return calendar.timeInMillis
    }

    /**
     * Converts Calendar's day number to the one stored in [DailyAlarm.daysMask].
     *
     * Calendar counts Sunday as 1 and Saturday as 7, but the mask treats bit 0 as
     * Monday. Passing Calendar's number straight through shifts every alarm by a
     * day, so a weekday alarm rings Sunday to Thursday and never on Friday.
     */
    private fun isoDayOfWeek(calendar: Calendar): Int {
        val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
        return if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
    }

    fun schedule(context: Context, alarm: DailyAlarm) {
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context, alarm.id)
        if (!alarm.enabled) return

        // No days ticked means the alarm can never ring, so nothing is scheduled.
        val triggerAt = nextTriggerAt(alarm) ?: return

        val pi = pendingIntent(context, alarm)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // A late wake-up helps; a missed one does not.
            am.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                5 * 60 * 1000L,
                pi
            )
        }
        trackKey(context, alarm.id.toString())
    }

    private fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, DailyAlarmReceiver::class.java).apply { action = ACTION },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun cancel(context: Context, alarm: DailyAlarm) = cancel(context, alarm.id)

    /** Re-arms every enabled alarm. Called after any alarm edit and after boot. */
    fun rescheduleAll(context: Context, alarms: List<DailyAlarm>) {
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldKeys = (prefs.getStringSet(KEYS, emptySet()) ?: emptySet()).toMutableSet()
        val keep = alarms.filter { it.enabled }.map { it.id.toString() }.toSet()

        // remove() while iterating the same set throws ConcurrentModificationException,
        // which crashed the app on every launch that re-armed alarms. Collect the
        // dead keys first, then drop them.
        val deadKeys = oldKeys.filter { !keep.contains(it) }
        for (key in deadKeys) {
            cancel(context, key.toLongOrNull() ?: 0L)
            oldKeys.remove(key)
        }
        for (alarm in alarms) {
            schedule(context, alarm)
            oldKeys.add(alarm.id.toString())
        }
        prefs.edit().putStringSet(KEYS, oldKeys).apply()
    }

    /**
     * Clears only this feature's alarms.
     *
     * Deliberately does not call `am.cancelAll()`, which would also wipe the class
     * reminders sharing the same AlarmManager. Every alarm here is tracked by id,
     * so cancelling them one by one is both correct and complete.
     */
    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (key in prefs.getStringSet(KEYS, emptySet()) ?: emptySet()) {
            cancel(context, key.toLongOrNull() ?: 0L)
        }
        prefs.edit().putStringSet(KEYS, emptySet()).apply()
    }

    private fun trackKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keys = (prefs.getStringSet(KEYS, emptySet()) ?: emptySet()).toMutableSet()
        keys.add(key)
        prefs.edit().putStringSet(KEYS, keys).apply()
    }

    fun showNotification(
        context: Context,
        alarmId: Long,
        label: String,
        vibrate: Boolean,
        sound: Boolean,
        soundUri: String? = null
    ) {
        ensureChannel(context)

        // A wake-up alarm should be heard even in silent mode, so it goes to the
        // stream volume rather than the notification volume the student muted.
        if (sound) {
            val audio = context.getSystemService(AudioManager::class.java)
            val original = audio?.getStreamVolume(AudioManager.STREAM_ALARM)
            audio?.setStreamVolume(
                AudioManager.STREAM_ALARM,
                (original ?: 0).coerceAtLeast(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM) * 3 / 5),
                0
            )
        }

        // Tapping the notification goes to the app, but the full-screen intent is
        // what launches over the lock screen when the phone is face down or asleep.
        val open = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            Intent(context, com.saintchigos.studyhub.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ring = PendingIntent.getActivity(
            context,
            alarmId.hashCode() xor RING_REQUEST,
            Intent(context, AlarmRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_SOUND, sound)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() xor SNOOZE_REQUEST,
            Intent(context, DailyAlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_SOUND, sound)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismiss = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() xor DISMISS_REQUEST,
            Intent(context, DailyAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label.ifBlank { "Wake-up alarm" })
            .setContentText("Open StudyHub to start the day.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setFullScreenIntent(ring, true)
            .setDefaults(0)
            .addAction(R.drawable.ic_notification, "Snooze $SNOOZE_MINUTES min", snooze)
            .addAction(R.drawable.ic_notification, "Dismiss", dismiss)

        // Sound and vibration come from the channel, so no per-post overrides here.
        // Overriding them again would drop the alarm stream volume boost.
        if (vibrate) body.setVibrate(longArrayOf(0, 500, 300, 500))

        try {
            NotificationManagerCompat.from(context).notify(alarmId.hashCode(), body.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }

    fun formatTime(minuteOfDay: Int): String {
        val hour24 = minuteOfDay / 60
        val minute = minuteOfDay % 60
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "%d:%02d %s".format(hour12, minute, suffix)
    }

    fun daySummary(daysMask: Int): String = when {
        daysMask == DailyAlarm.ALL_DAYS -> "Every day"
        daysMask == DailyAlarm.WEEKDAYS -> "Weekdays"
        daysMask == (DailyAlarm.WEEKDAYS or (1 shl 5)) -> "Mon to Sat"
        else -> DAY_NAMES
            .mapIndexedNotNull { index, name ->
                if (DailyAlarm.hasDay(daysMask, index + 1)) name else null
            }
            .joinToString(" ")
    }

    private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}
