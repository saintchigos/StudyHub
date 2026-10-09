package com.saintchigos.studyhub.ui.components

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.saintchigos.studyhub.reminder.DailyAlarms

/** What Android is currently allowing, read fresh each time the screen resumes. */
private data class Health(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val battery: Boolean
) {
    val problems: Int
        get() = listOf(notifications, exactAlarms, fullScreen, battery).count { !it }
}

private fun readHealth(context: Context): Health {
    val fullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
    } else {
        true
    }
    val battery = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    return Health(
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarms = DailyAlarms.canScheduleExact(context),
        fullScreen = fullScreen,
        battery = battery
    )
}

/**
 * Shows, in plain words, everything that can stop an alarm or reminder from reaching
 * the student, with a button that goes straight to the right system screen.
 *
 * Most "my alarm did not ring" reports are not code bugs but a switch Android turned
 * off on its own, and the student has no way to see which one. This makes them visible,
 * and the test button proves the whole path works while the phone is locked.
 */
@Composable
fun ReminderHealthCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var health by remember { mutableStateOf(readHealth(context)) }

    // Permissions change in system screens, so re-read whenever the student comes back.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) health = readHealth(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Alarm and reminder health", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (health.problems == 0) {
                "Everything Android controls is switched on."
            } else {
                "${health.problems} thing${if (health.problems == 1) "" else "s"} could stop alarms or reminders. Tap Fix on each."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (health.problems == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Spacer(Modifier.height(6.dp))

        HealthRow(
            ok = health.notifications,
            title = "Notifications",
            detail = "Needed to show any reminder.",
            onFix = { openNotificationSettings(context) }
        )
        HealthRow(
            ok = health.exactAlarms,
            title = "Exact alarms",
            detail = "Without this, alarms can arrive minutes late.",
            onFix = { openExactAlarmSettings(context) }
        )
        HealthRow(
            ok = health.fullScreen,
            title = "Alarm screen on the lock screen",
            detail = "Lets the alarm light up the screen while the phone is locked.",
            onFix = { openFullScreenSettings(context) }
        )
        HealthRow(
            ok = health.battery,
            title = "Battery optimisation off",
            detail = "Stops the phone putting StudyHub to sleep overnight.",
            onFix = { openBatterySettings(context) }
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "On Tecno, Infinix, Xiaomi and Oppo phones also turn on Autostart for " +
                "StudyHub and lock it in the recent apps list. Those switches are in the " +
                "phone's own settings, so the app cannot flip them for you.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                DailyAlarms.scheduleTest(context)
                Toast.makeText(
                    context,
                    "Test alarm rings in 15 seconds. Lock your phone now.",
                    Toast.LENGTH_LONG
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test the alarm in 15 seconds")
        }
    }
}

@Composable
private fun HealthRow(ok: Boolean, title: String, detail: String, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = if (ok) "Fine" else "Needs attention",
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!ok) {
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
}

private fun openFullScreenSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
}

private fun openBatterySettings(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    val opened = runCatching { context.startActivity(direct) }.isSuccess
    if (!opened) {
        // Some phones hide the direct prompt; the general list is always there.
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
