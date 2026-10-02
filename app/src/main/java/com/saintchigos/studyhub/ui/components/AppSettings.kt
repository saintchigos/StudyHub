package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel

private val FIRST_LEAD_OPTIONS = listOf(5, 10, 15, 20, 30)
private val LATER_LEAD_OPTIONS = listOf(3, 5, 10, 15)
private val THEME_OPTIONS = listOf(
    "system" to "System",
    "light" to "Light",
    "dark" to "Dark"
)

/** Appearance, reminder timing and alert style. */
@Composable
fun AppSettingsSection(
    viewModel: StudyHubViewModel
) {
    val firstLead by viewModel.firstClassLead.collectAsStateWithLifecycle()
    val laterLead by viewModel.otherClassLead.collectAsStateWithLifecycle()
    val sound by viewModel.reminderSound.collectAsStateWithLifecycle()
    val vibrate by viewModel.reminderVibrate.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Appearance", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            THEME_OPTIONS.forEach { (value, label) ->
                FilterChip(
                    selected = themeMode == value,
                    onClick = { viewModel.setThemeMode(value) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("First class of the day", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Warn me this long before my earliest class",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FIRST_LEAD_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = firstLead == minutes,
                    onClick = { viewModel.setFirstClassLead(minutes) },
                    label = { Text("$minutes min") }
                )
            }
        }

        Text("Every other class", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Warn me this long before the classes that follow",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LATER_LEAD_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = laterLead == minutes,
                    onClick = { viewModel.setOtherClassLead(minutes) },
                    label = { Text("$minutes min") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ToggleRow(
                    title = "Alert sound",
                    detail = "Play a sound with each class reminder",
                    checked = sound,
                    onCheckedChange = { viewModel.setReminderSound(it) }
                )
                ToggleRow(
                    title = "Vibration",
                    detail = "Vibrate the phone so the alert is hard to miss",
                    checked = vibrate,
                    onCheckedChange = { viewModel.setReminderVibrate(it) }
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { viewModel.sendTestReminder() }) {
                    Text("Send a test reminder")
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}