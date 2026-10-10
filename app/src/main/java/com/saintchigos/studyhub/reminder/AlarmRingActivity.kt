package com.saintchigos.studyhub.reminder

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.ui.theme.StudyHubTheme

/**
 * The full-screen alarm view.
 *
 * A notification on its own is easy to swipe away half asleep, and on some
 * launchers it never even appears because the phone is locked. This takes over the
 * whole screen so the alarm cannot be slept through.
 *
 * It deliberately owns no audio. [AlarmService] holds the ringtone, which is what
 * makes the alarm survive this screen being swiped away or the phone being
 * unlocked. Buttons here ask the service to stop.
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake the display and ask the keyguard to step aside, so the student sees
        // the alarm instead of only feeling a vibration in their pocket.
        showOverLockScreen()

        val label = intent?.getStringExtra(DailyAlarms.EXTRA_LABEL).orEmpty()
        val alarmId = intent?.getLongExtra(DailyAlarms.EXTRA_ALARM_ID, 0L) ?: 0L

        // The ringing itself belongs to AlarmService, which survives this screen
        // being swiped away or the phone being unlocked. This activity is only the
        // full-screen view of an alarm that is already sounding, so it starts no
        // audio of its own: two ringers would fight and only one would be stoppable.

        setContent {
            StudyHubTheme {
                AlarmRingScreen(
                    label = label.ifBlank { "Wake-up alarm" },
                    onTurnOff = {
                        AlarmService.stop(applicationContext)
                        DailyAlarms.dismiss(applicationContext, alarmId)
                        finishAndRemoveTask()
                    },
                    onSnooze = {
                        AlarmService.snooze(
                            applicationContext,
                            alarmId,
                            label,
                            intent?.getBooleanExtra(DailyAlarms.EXTRA_VIBRATE, true) ?: true,
                            intent?.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true) ?: true,
                            intent?.getStringExtra(DailyAlarms.EXTRA_SOUND_URI)
                        )
                        DailyAlarms.dismiss(applicationContext, alarmId)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    private fun showOverLockScreen() {
        // Deliberately does not call requestDismissKeyguard. On a phone with a PIN or
        // pattern that raises the unlock prompt on top of the alarm, so the student
        // saw a lock screen instead of Turn off and Snooze. Showing over the keyguard
        // is all an alarm needs; the buttons work without unlocking.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // A second alarm while this screen is up should not leave buttons that act on
        // the first one.
        setIntent(intent)
    }
}

/**
 * The alarm screen itself.
 *
 * Two buttons of equal size and weight: a student who cannot read the screen well
 * yet should be able to act by position, and neither choice should look like the
 * "wrong" one. The pulsing rings are there to be noticed half asleep, and they are the
 * only motion on the screen so nothing competes with the two buttons.
 */
@Composable
private fun AlarmRingScreen(
    label: String,
    onTurnOff: () -> Unit,
    onSnooze: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val onColor = scheme.onPrimary

    val transition = rememberInfiniteTransition(label = "alarmPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primary, lerp(scheme.primary, scheme.tertiary, 0.7f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((150 * pulse).dp)
                        .clip(CircleShape)
                        .background(onColor.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(onColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = onColor
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = DailyAlarms.formatTime(currentMinuteOfDay()),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onColor
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = onColor.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Stacked rather than side by side: at 360dp wide, two 72dp buttons
            // plus 24dp of gap leave each label truncated to "Turn" and "Snoo".
            Button(
                onClick = onTurnOff,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onColor,
                    contentColor = scheme.primary
                )
            ) {
                Text(
                    "Turn off",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onColor.copy(alpha = 0.16f),
                    contentColor = onColor
                )
            ) {
                Text(
                    "Snooze ${DailyAlarms.SNOOZE_MINUTES} min",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun currentMinuteOfDay(): Int {
    val calendar = java.util.Calendar.getInstance()
    return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
        calendar.get(java.util.Calendar.MINUTE)
}