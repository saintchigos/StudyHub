package com.saintchigos.studyhub.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.saintchigos.studyhub.MainActivity
import com.saintchigos.studyhub.R

/**
 * Owns the actual ringing.
 *
 * The first version played the alarm from the notification channel, which meant
 * the sound belonged to a notification. Unlocking the phone dismissed that
 * notification and the alarm went silent, which is the one thing a wake-up alarm
 * must never do. A foreground service is the fix: it holds the media player and the
 * vibrator itself, keeps an ongoing notification that cannot be swiped away, and
 * only stops when the student explicitly turns it off or snoozes.
 *
 * Three extra layers, because OEM builds (this one is Transsion) routinely kill a
 * foreground service within seconds of it starting:
 *
 *  1. A [MediaSessionCompat] is published. Without one, `mediaPlayback` foreground
 *     services are treated as media apps that are not playing anything, and some
 *     OEM power managers kill them on sight.
 *  2. The ringing state is written to preferences, so a service restarted with a
 *     null intent by the system picks the alarm back up instead of going quiet.
 *  3. A repeating tick re-asserts the alarm volume. Without it, pressing the volume
 *     key down during the night silences the alarm, which is exactly what the
 *     student half asleep at 06:00 will do by accident.
 *
 * A separate channel is used rather than [DailyAlarms.CHANNEL_ID] so the channel's
 * own sound does not fight the service's looping tone. The service is the only
 * thing that makes noise, which means muting app notifications no longer silences
 * the alarm.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSession? = null

    private var alarmId = 0L
    private var label = ""
    private var vibrate = true
    private var sound = true
    private var soundUri: String? = null

    /** See [EXTRA_RAISE_SCREEN]. False on watchdog re-arms. */
    private var raiseScreen = true

    /**
     * Set once the student turns it off or snoozes, so a restart cannot resume it.
     *
     * Process-wide rather than per-instance because "Turn off" can arrive through
     * the static [stop], which destroys this instance without ever calling
     * [onStartCommand] with [ACTION_STOP]. Without a shared flag the service would
     * read its own saved state in [onDestroy] and restart, leaving an alarm that
     * cannot be silenced.
     */
    @Volatile
    private var dismissed = dismissedGlobally

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Re-raises the alarm volume and restarts the tone if something interrupted it.
     *
     * Runs every few seconds for as long as the alarm is ringing.
     */
    private val reinforce = object : Runnable {
        override fun run() {
            if (dismissed) return
            forceAlarmVolume()
            ensureTonePlaying()
            ensureVibrating()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                dismissGlobally()
                stopEverything()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                dismissGlobally()
                val id = intent.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, alarmId)
                val snoozeLabel = intent.getStringExtra(DailyAlarms.EXTRA_LABEL) ?: label
                val snoozeVibrate =
                    intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, vibrate)
                val snoozeSound = intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, sound)
                val snoozeUri = intent.getStringExtra(EXTRA_SOUND_URI)
                stopEverything()
                stopSelf()
                DailyAlarms.snooze(
                    this, id, snoozeLabel, snoozeVibrate, snoozeSound, snoozeUri
                )
                return START_NOT_STICKY
            }
        }

        // A null intent means the system restarted the service after killing it. The
        // alarm is still supposed to be ringing, so the state was saved on disk.
        if (intent == null) {
            if (!restoreRinging()) {
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            alarmId = intent.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, 0L)
            label = intent.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty()
            vibrate = intent.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true)
            sound = intent.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true)
            soundUri = intent.getStringExtra(EXTRA_SOUND_URI)
            raiseScreen = intent.getBooleanExtra(EXTRA_RAISE_SCREEN, true)
            dismissed = false
            dismissedGlobally = false
            persistRinging()
        }

        // startForegroundService gives a short window to post the notification, so
        // this has to come before anything slow.
        startForegroundNow()
        startRinging()
        // Only the first ring of an alarm grabs the screen. Every later launch is
        // either a watchdog re-arm or a service rebuild, where the ring screen is
        // already up and re-raising it only fights with the shade.
        if (shouldTakeOverScreen()) launchRingScreen()

        handler.removeCallbacks(reinforce)
        handler.post(reinforce)

        // Sticky, not redeliver: the ringing state lives in preferences, so the
        // service can rebuild itself from a null intent after an OEM kill.
        return START_STICKY
    }

    private fun startForegroundNow() {
        ensureChannel(this)
        startMediaSession()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }.onFailure {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Publishes a media session.
     *
     * Purely for the platform's benefit: `mediaPlayback` services without one look
     * like media apps playing nothing, and OEM power managers reap them.
     */
    private fun startMediaSession() {
        if (mediaSession != null) return
        val session = MediaSession(this, "StudyHub:alarm")
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                .build()
        )
        runCatching { session.isActive = true }
        mediaSession = session
    }

    private fun startRinging() {
        forceAlarmVolume()
        ensureTonePlaying()
        ensureVibrating()

        // Keeps the CPU alive even if the screen is off and untouched.
        if (wakeLock?.isHeld != true) {
            wakeLock = getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudyHub:alarm")
                ?.apply {
                    setReferenceCounted(false)
                    acquire(MAX_WAKE_LOCK_MS)
                }
        }
    }

    /**
     * Pushes the alarm stream as loud as it will go.
     *
     * Called on every tick, so pressing volume down during the night does not
     * silence a wake-up alarm.
     */
    private fun forceAlarmVolume() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return
        runCatching {
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
        }
    }

    /** Starts the looping tone, or restarts it if the player was stopped. */
    private fun ensureTonePlaying() {
        if (!sound) return
        val existing = player
        if (existing != null) {
            runCatching {
                if (!existing.isPlaying) existing.start()
            }
            return
        }

        val uri = resolveSoundUri()
        if (uri == null) return

        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                setOnErrorListener { _, _, _ ->
                    // A bad custom tone must not take the alarm down with it.
                    player = null
                    true
                }
                prepare()
                start()
            }
        }.getOrNull()
    }

    /**
     * The student's chosen tone, falling back to the system alarm.
     *
     * A stored URI that no longer resolves (deleted file, revoked permission) is
     * skipped rather than allowed to fail the alarm.
     */
    private fun resolveSoundUri(): Uri? {
        soundUri?.let { stored ->
            val uri = Uri.parse(stored)
            val resolved = runCatching {
                RingtoneManager.getRingtone(this, uri)
            }.getOrNull()
            if (resolved != null) return uri
        }
        return RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    /**
     * Ramps up in four stages instead of buzzing flatly for ten minutes.
     *
     * A steady buzz is what you learn to sleep through. Each cycle is louder and
     * more insistent, so someone who did not move on the first pulse moves on the
     * fourth. The loop is infinite (repeat index 0), so it never simply stops.
     */
    private fun ensureVibrating() {
        if (!vibrate || vibrator != null) return
        val service = getSystemService(Vibrator::class.java) ?: return
        if (!service.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                service.vibrate(
                    VibrationEffect.createWaveform(DEEP_SLEEP_PATTERN, DEEP_SLEEP_AMPLITUDES, 0)
                )
            } else {
                service.vibrate(VibrationEffect.createWaveform(DEEP_SLEEP_PATTERN, 0))
            }
            vibrator = service
        }
    }

    private fun stopEverything() {
        handler.removeCallbacks(reinforce)

        runCatching { player?.stop() }
        player?.release()
        player = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null

        runCatching { mediaSession?.isActive = false }
        mediaSession?.release()
        mediaSession = null

        clearPersistedRinging()
        NotificationManagerCompatHelper.cancel(this, NOTIFICATION_ID)
    }

    override fun onDestroy() {
        // onDestroy also fires when the OEM reaps the service. If the student never
        // turned it off, the alarm is meant to still be ringing, so it is restarted
        // rather than dropped on the floor.
        val shouldResume = !dismissed && hasPersistedRinging()
        stopEverything()
        if (shouldResume) {
            runCatching {
                val intent = Intent(this, AlarmService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away must not silence a wake-up alarm.
        if (!dismissed) AlarmService.start(this, alarmId, label, vibrate, sound, soundUri)
        super.onTaskRemoved(rootIntent)
    }

    // ---- Surviving an OEM kill -------------------------------------------------

    private fun persistRinging() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RINGING, true)
            .putLong(KEY_ALARM_ID, alarmId)
            .putString(KEY_LABEL, label)
            .putBoolean(KEY_VIBRATE, vibrate)
            .putBoolean(KEY_SOUND, sound)
            .putString(KEY_SOUND_URI, soundUri)
            .apply()
    }

    private fun clearPersistedRinging() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RINGING, false)
            .apply()
    }

    private fun hasPersistedRinging(): Boolean =
        getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_RINGING, false)

    private fun restoreRinging(): Boolean {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_RINGING, false)) return false
        alarmId = prefs.getLong(KEY_ALARM_ID, 0L)
        label = prefs.getString(KEY_LABEL, "").orEmpty()
        vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        sound = prefs.getBoolean(KEY_SOUND, true)
        soundUri = prefs.getString(KEY_SOUND_URI, null)
        return true
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stop = PendingIntent.getService(
            this,
            alarmId.hashCode() xor STOP_REQUEST,
            Intent(this, AlarmService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze = PendingIntent.getService(
            this,
            alarmId.hashCode() xor SNOOZE_REQUEST,
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(DailyAlarms.EXTRA_ALARM_ID, alarmId)
                putExtra(DailyAlarms.EXTRA_LABEL, label)
                putExtra(DailyAlarms.EXTRA_VIBRATE, vibrate)
                putExtra(DailyAlarms.EXTRA_SOUND, sound)
                putExtra(EXTRA_SOUND_URI, soundUri)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ongoing with no sound on the channel: the service makes the noise, and an
        // ongoing notification cannot be swiped away to silence the alarm.
        //
        // The full-screen intent rides on the notification rather than being sent
        // directly, because a direct send from a service is blocked by the
        // background-activity rules. Carrying it on a notification is the path
        // Android actually honours when waking the screen.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label.ifBlank { DEFAULT_LABEL })
            .setContentText("Ringing. It only stops when you turn it off or snooze.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(open)
            .setFullScreenIntent(ringScreenIntent(), shouldTakeOverScreen())
            .setDefaults(0)
            .addAction(R.drawable.ic_notification, "Snooze $SNOOZE_LABEL min", snooze)
            .addAction(R.drawable.ic_notification, "Turn off", stop)
            .build()
    }

    /**
     * Whether the alarm should grab the screen, not just make a noise.
     *
     * Two conditions have to hold, and both matter:
     *
     * 1. The phone must actually be locked. Waking a screen someone is already
     *    using to interrupt them is rude and pointless - the noise is enough.
     * 2. This is the first ring of this alarm, not a watchdog re-arm.
     *
     * Without (2) this OEM leaves the notification shade stuck open and
     * undismissable, which happened when the watchdog re-raised a full-screen
     * intent every thirty seconds. A watchdog exists to keep the *noise* going, so
     * it deliberately passes no screen request and only restarts the ringing.
     */
    private fun shouldTakeOverScreen(): Boolean =
        raiseScreen &&
            !dismissedGlobally &&
            isScreenLocked()

    private fun isScreenLocked(): Boolean {
        val keyguard = getSystemService(android.app.KeyguardManager::class.java)
        if (keyguard != null && keyguard.isKeyguardLocked) return true
        // A dark screen needs waking even when the keyguard is not engaged, for
        // example on a phone whose lock screen has already timed out.
        val power = getSystemService(android.os.PowerManager::class.java)
        return power != null && !power.isInteractive
    }

    /**
     * The intent that takes over the screen with the two buttons.
     *
     * Attached to the notification as a full-screen intent. Android 14 may still
     * refuse it for apps it does not recognise as alarm apps, which is why the
     * service keeps ringing independently: the noise never depends on this
     * launching.
     */
    private fun ringScreenIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        alarmId.hashCode() xor RING_REQUEST,
        Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(DailyAlarms.EXTRA_ALARM_ID, alarmId)
            putExtra(DailyAlarms.EXTRA_LABEL, label)
            putExtra(DailyAlarms.EXTRA_VIBRATE, vibrate)
            putExtra(DailyAlarms.EXTRA_SOUND, sound)
            putExtra(EXTRA_SOUND_URI, soundUri)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Also tries to show the screen immediately, without waiting for the user to
     * pull the shade down. Harmless when it is refused.
     */
    private fun launchRingScreen() {
        runCatching { ringScreenIntent().send() }
    }

    companion object {
        const val CHANNEL_ID = "daily_alarms_ongoing"
        const val EXTRA_SOUND_URI = "sound_uri"

        /**
         * Whether this launch may pull the ring screen up over the lock screen.
         *
         * False for watchdog re-arms: they exist to restore the noise after this
         * phone reaps the service, and re-raising a full-screen intent every thirty
         * seconds wedges the notification shade open on this OEM.
         */
        const val EXTRA_RAISE_SCREEN = "raise_screen"

        /** Used when the alarm has no label of its own. */
        const val DEFAULT_LABEL = "Wake up Mr Chigos"

        private const val NOTIFICATION_ID = 0x414C

        const val ACTION_STOP = "com.saintchigos.studyhub.ALARM_STOP"
        const val ACTION_SNOOZE = "com.saintchigos.studyhub.ALARM_SNOOZE"

        private const val STOP_REQUEST = 0x60
        private const val SNOOZE_REQUEST = 0x61
        private const val RING_REQUEST = 0x62

        private const val PREFS = "alarm_service"
        private const val KEY_RINGING = "ringing"
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_LABEL = "label"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_SOUND = "sound"
        private const val KEY_SOUND_URI = "sound_uri"

        private val SNOOZE_LABEL = DailyAlarms.SNOOZE_MINUTES

        private val VIBRATE_PATTERN = longArrayOf(0, 600, 400)

        /**
         * The deep-sleeper pattern: four escalating pulses, then a short rest.
         *
         * Timings are wait/on/wait/on... so the numbers read as [off, on, off, on].
         * Each burst gets longer and the gap between them gets shorter, so the
         * alarm feels like it is insisting rather than idling.
         */
        private val DEEP_SLEEP_PATTERN = longArrayOf(
            0, 400, 900,    // stage 1: polite
            600, 700, 600,   // stage 2: firmer
            350, 1100, 300,  // stage 3: insistent
            200, 1600, 200   // stage 4: as loud as the phone will go
        )

        /**
         * Matching strength for each burst, 1-255.
         *
         * Scaling by strength matters for sleepers using a phone on a mattress: the
         * vibration is transmitted through the bed rather than held in a hand, so a
         * flat maximum can wake a partner instead. Starting gentle and rising only
         * reaches the sleeper.
         */
        private val DEEP_SLEEP_AMPLITUDES = intArrayOf(
            0, 90, 0,
            0, 160, 0,
            0, 210, 0,
            0, 255
        )

        /** How often volume is pushed back up and the tone checked. */
        private const val TICK_MS = 4_000L

        /** Safety cap so a leaked wake lock cannot drain the battery all day. */
        private const val MAX_WAKE_LOCK_MS = 15 * 60_000L

        private const val TAG = "StudyHubAlarm"

        /**
         * Process-wide "the student stopped this" flag.
         *
         * Read by [onDestroy] to decide whether a dying service should come back.
         * It has to outlive the instance, because stopping the alarm destroys the
         * service without going through [onStartCommand].
         */
        @Volatile
        private var dismissedGlobally = false

        private fun dismissGlobally() {
            dismissedGlobally = true
        }

        fun ensureChannel(context: Context) {
            val manager =
                context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Ringing alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shown while an alarm is ringing"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    // Silent: the foreground service plays the alarm itself, so
                    // muting this channel cannot silence a wake-up alarm.
                    setSound(null, null)
                    enableVibration(false)
                    setBypassDnd(true)
                }
            )
        }

        /** Starts ringing. Safe to call again while already ringing. */
        fun start(
            context: Context,
            alarmId: Long,
            label: String,
            vibrate: Boolean,
            sound: Boolean,
            soundUri: String? = null,
            raiseScreen: Boolean = true
        ) {
            ensureChannel(context)
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(DailyAlarms.EXTRA_ALARM_ID, alarmId)
                putExtra(DailyAlarms.EXTRA_LABEL, label)
                putExtra(DailyAlarms.EXTRA_VIBRATE, vibrate)
                putExtra(DailyAlarms.EXTRA_SOUND, sound)
                putExtra(EXTRA_SOUND_URI, soundUri)
                putExtra(EXTRA_RAISE_SCREEN, raiseScreen)
            }
            // Wrapped because a foreground service can be refused on some OEM
            // builds; the notification below is the fallback so the alarm is never
            // silent.
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure {
                Log.w(TAG, "Foreground service refused, falling back to notification", it)
                DailyAlarms.showNotification(
                    context, alarmId, label, vibrate, sound, soundUri
                )
            }
        }

        /**
         * Stops the alarm.
         *
         * Does the work here rather than sending a command to the running service:
         * `startService` is asynchronous, so a following `stopService` could destroy
         * the service before it ever handled the intent, leaving the ringtone playing
         * with no owner. Stopping directly cannot lose the command.
         */
        fun stop(context: Context) {
            // Ends the watchdog loop before stopping, so the alarm can actually be
            // silenced and does not immediately restart itself.
            dismissedGlobally = true
            DailyAlarms.clearRinging(context)
            context.stopService(Intent(context, AlarmService::class.java))
            NotificationManagerCompatHelper.cancel(context, NOTIFICATION_ID)
        }

        /** Schedules the snooze and silences the current ring in one step. */
        fun snooze(
            context: Context,
            alarmId: Long,
            label: String,
            vibrate: Boolean,
            sound: Boolean,
            soundUri: String? = null
        ) {
            DailyAlarms.snooze(context, alarmId, label, vibrate, sound, soundUri)
            stop(context)
        }
    }
}

/** Tiny wrapper so the service does not need the core-ktx import dance twice. */
private object NotificationManagerCompatHelper {
    fun cancel(context: Context, id: Int) {
        context.getSystemService(NotificationManager::class.java)?.cancel(id)
    }
}