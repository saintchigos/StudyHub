package com.saintchigos.studyhub.reminder

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        setTurnScreenOn(true)

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
                            intent?.getBooleanExtra(DailyAlarms.EXTRA_SOUND, true) ?: true
                        )
                        DailyAlarms.dismiss(applicationContext, alarmId)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
            // Show over the keyguard from code as well as in the manifest, so the
            // alarm is visible even when Android refuses the full-screen intent.
            setShowWhenLocked(true)
        }
    }
}

/**
 * The alarm screen itself.
 *
 * Two buttons of equal size and weight: a student who cannot read the screen well
 * yet should be able to act by position, and neither choice should look like the
 * "wrong" one.
 */
@Composable
private fun AlarmRingScreen(
    label: String,
    onTurnOff: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Alarm,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = DailyAlarms.formatTime(currentMinuteOfDay()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Turn off", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    "Snooze ${DailyAlarms.SNOOZE_MINUTES} min",
                    style = MaterialTheme.typography.titleLarge
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