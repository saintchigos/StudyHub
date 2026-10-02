package com.saintchigos.studyhub.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** WhatsApp support line for Chigos Media. */
private const val SUPPORT_WHATSAPP = "26662848760"

/**
 * Lets the student turn class reminders on and off, and explains what is blocking
 * them if the OS is preventing delivery.
 */
@Composable
fun NotificationSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsEnabled by remember { mutableStateOf(true) }
    var exactAllowed by remember { mutableStateOf(true) }

    fun refresh() {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val am = context.getSystemService(AlarmManager::class.java)
        exactAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    // Permission can change while the app is backgrounded, so re-read on resume.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Class reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "10 minutes before your first class of the day, " +
                            "5 minutes before every other class.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { wanted ->
                        if (wanted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openAppNotificationSettings(context)
                            }
                        } else {
                            openAppNotificationSettings(context)
                        }
                    }
                )
            }

            if (!notificationsEnabled) {
                Spacer(Modifier.height(8.dp))
                WarningLine(
                    text = "Reminders are blocked, so you will not be warned before class."
                )
                TextButton(onClick = { openAppNotificationSettings(context) }) {
                    Text("Open notification settings")
                }
            }

            if (!exactAllowed) {
                Spacer(Modifier.height(4.dp))
                WarningLine(text = "Android cannot schedule precise alarms right now.")
                TextButton(onClick = { openExactAlarmSettings(context) }) {
                    Text("Allow alarms and reminders")
                }
            }
        }
    }
}

@Composable
private fun WarningLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/** Opens WhatsApp chat with Chigos Media support. */
@Composable
fun SupportButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Tech help and support", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Stuck with something? Message Chigos Media on WhatsApp and we will help you sort it out.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = { openWhatsApp(context) }) { Text("Chat on WhatsApp") }
            TextButton(onClick = { openDialer(context) }) { Text("Call +266 6284 8760") }
        }
    }
}

@Composable
fun PoweredBy(modifier: Modifier = Modifier) {
    Text(
        text = "Powered by Chigos Media",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 6.dp)
    )
}

private fun openWhatsApp(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$SUPPORT_WHATSAPP"))
    runCatching { context.startActivity(intent) }
}

private fun openDialer(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$SUPPORT_WHATSAPP"))
    runCatching { context.startActivity(intent) }
}

private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
}