package com.saintchigos.studyhub.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.theme.Accent
import com.saintchigos.studyhub.ui.theme.accentSchemes

/** Material You only exists from Android 12, so the option is hidden before that. */
private val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val amoled by viewModel.amoled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val nextClass by viewModel.nextClassNotification.collectAsStateWithLifecycle()
    val deadlines by viewModel.deadlineReminders.collectAsStateWithLifecycle()
    val briefing by viewModel.morningBriefing.collectAsStateWithLifecycle()

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

        // The wallpaper option only means anything from Android 12, so it is hidden
        // rather than shown as a switch that would silently do nothing.
        if (dynamicColorAvailable) {
            ToggleRow(
                title = "Wallpaper colours",
                detail = "Match the app to your phone's theme",
                checked = dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) }
            )
        }

        // Hidden while the wallpaper is winning, because picking an accent then would
        // look like it had done nothing.
        if (!dynamicColor || !dynamicColorAvailable) {
            Text(
                "Colour",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            AccentPicker(
                selected = accent,
                onSelect = { viewModel.setAccent(it) }
            )
        }

        ToggleRow(
            title = "Extra dark",
            detail = if (amoled) "Pure black. Saves battery on OLED screens" else "Softer greys in the dark",
            checked = amoled,
            onCheckedChange = { viewModel.setAmoled(it) }
        )

        TextSizeRow(
            scale = fontScale,
            onChange = { viewModel.setFontScale(it) }
        )

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
                ToggleRow(
                    title = "Task and exam reminders",
                    detail = "A warning a day ahead and a few hours before something is due",
                    checked = deadlines,
                    onCheckedChange = { viewModel.setDeadlineReminders(it) }
                )
                ToggleRow(
                    title = "Morning briefing",
                    detail = "A short summary of your day at 7:00, only on days with something in them",
                    checked = briefing,
                    onCheckedChange = { viewModel.setMorningBriefing(it) }
                )
                ToggleRow(
                    title = "Show what's next",
                    detail = "A silent card on your lock screen with the next class and how long until it",
                    checked = nextClass,
                    onCheckedChange = { viewModel.setNextClassNotification(it) }
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { viewModel.sendTestReminder() }) {
                    Text("Send a test reminder")
                }
            }
        }
    }
}

/**
 * Swatches for each accent.
 *
 * Each swatch previews the real generated scheme rather than the raw seed, because the
 * seed itself is never shown anywhere in the app. A student picking a colour wants to
 * see the colour the app will actually become.
 */
@Composable
private fun AccentPicker(
    selected: Accent,
    onSelect: (Accent) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Accent.entries.forEach { option ->
            val scheme = remember(option, isDark) {
                val (light, dark) = accentSchemes(option)
                if (isDark) dark else light
            }
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(option) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (isSelected) "In use" else "Tap to use",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Text size, with the same sample text at the real chosen size.
 *
 * The scale applies to the whole app, so the preview has to move with it or the
 * student is judging the wrong thing.
 */
@Composable
private fun TextSizeRow(
    scale: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TextDecrease, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = "Text size",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Filled.TextIncrease, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Slider(
            value = scale,
            onValueChange = onChange,
            valueRange = 0.85f..1.5f,
            // One step per 5%, which is about the smallest change a person can see.
            steps = 12
        )
        Text(
            text = "Classes today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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