package com.saintchigos.studyhub.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.saintchigos.studyhub.MainActivity
import com.saintchigos.studyhub.R
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.StudyHubPrefs
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDateTime

/**
 * A silent, permanent notification that always says what is coming up next.
 *
 * A student glancing at their phone should not have to open the app to find out
 * whether they can sleep another twenty minutes. This puts "Calculus in 25 min, A12"
 * on the lock screen and in the shade, updated whenever the timetable changes.
 *
 * It is deliberately silent and low importance. It is information the student already
 * has, so it must never buzz, and it sits on its own channel precisely so that
 * changing its importance later cannot affect the class reminders or the wake-up
 * alarm, which have to stay loud.
 *
 * Only one notification is ever posted, and it is refreshed by [update] rather than
 * by a repeating timer, so an app left open overnight does not wake the radio.
 */
object NextClass {

    const val CHANNEL_ID = "next_class_v1"
    private const val NOTIFICATION_ID = 100

    /**
     * Posted when there is nothing scheduled.
     *
     * Silence would be ambiguous: no notification at all looks like the feature is
     * broken, whereas "no more classes today" is genuinely reassuring at 9pm.
     */
    private const val IDLE_TITLE = "No more classes today"
    private const val IDLE_BODY = "Your timetable is clear. Rest."

    /** Idempotent, so it is safe to call on every launch. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "What's next",
                // MIN rather than LOW: no heads-up, no sound, and it does not count as
                // a notification the student has to deal with.
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Shows the class you are heading to next"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Removes it, used when the student turns the setting off. */
    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /**
     * Works out the next session from now and posts it.
     *
     * Honours the student's switch here rather than in the callers, so there is no
     * path that can post the card after they turned it off.
     *
     * @param sessions every weekly class, in any order.
     */
    fun update(context: Context, sessions: List<SessionWithCourse>, now: LocalDateTime = TimeUtil.now()) {
        if (!StudyHubPrefs(context).nextClassNotification) {
            cancel(context)
            return
        }

        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val next = nextSession(sessions, now)

        if (next == null) {
            // Nothing left to walk to. Whether that means "your day is done" or "you
            // have no timetable" changes what the card should say, and the difference
            // matters: "no more classes today" at 6pm is welcome, at 3am it is noise,
            // and a missing timetable should not be papered over with a message about
            // resting.
            val hadClassesToday = sessions.any { it.dayOfWeek == now.dayOfWeek.value }
            when {
                sessions.isEmpty() -> cancel(context)
                hadClassesToday -> manager.notify(NOTIFICATION_ID, build(context, IDLE_TITLE, IDLE_BODY))
                else -> cancel(context)
            }
            return
        }

        val (session, minutesAway) = next
        val code = session.courseCode.ifBlank { session.courseName }
        val whenLabel = if (minutesAway < 60) {
            "in $minutesAway min"
        } else {
            TimeUtil.relativeLabel(
                System.currentTimeMillis() + minutesAway * 60_000L
            ).removePrefix("in ")
                .let { "in $it" }
        }

        val room = session.room.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        manager.notify(
            NOTIFICATION_ID,
            build(
                context = context,
                title = "$code $whenLabel$room",
                body = session.courseName
            )
        )
    }

    /**
     * The next class at or after [now], with how many minutes away it is.
     *
     * Returns null when nothing is left, which is different from "nothing scheduled
     * at all": the caller needs the distinction to decide between saying the week is
     * over and saying the day is over.
     */
    fun nextSession(
        sessions: List<SessionWithCourse>,
        now: LocalDateTime = TimeUtil.now()
    ): Pair<SessionWithCourse, Long>? =
        sessions
            .map { it to nextOccurrenceMinutes(it, now) }
            .filter { it.second >= 0 }
            .minByOrNull { it.second }

    /** Minutes from now until this weekly class next happens, or -1 if already past. */
    private fun nextOccurrenceMinutes(session: SessionWithCourse, now: LocalDateTime): Long {
        val todayIso = now.dayOfWeek.value
        var deltaDays = (session.dayOfWeek - todayIso + 7) % 7
        if (deltaDays == 0) {
            val start = TimeUtil.minuteOfDayToLocalTime(session.startMinute)
            if (!start.isAfter(now.toLocalTime())) {
                // Already started. Not "next", but still worth showing as ended.
                return -1
            }
        }
        val target = now.toLocalDate().plusDays(deltaDays.toLong())
            .atTime(TimeUtil.minuteOfDayToLocalTime(session.startMinute))
        return java.time.Duration.between(now, target).toMinutes()
    }

    private fun build(context: Context, title: String, body: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            // No sound, no vibration, no alert: this is a status, not an event.
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .build()
    }
}