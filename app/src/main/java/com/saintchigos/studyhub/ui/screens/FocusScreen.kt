package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.DailyAlarm
import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.reminder.DailyAlarms
import com.saintchigos.studyhub.ui.FocusPhase
import com.saintchigos.studyhub.ui.FocusViewModel

private val FOCUS_PRESETS = listOf(15 to "Quick review", 25 to "Deep work", 45 to "Long block", 60 to "Exam grind")

private val PRESET_LABELS = listOf("Deep work", "Revision", "Homework", "Reading", "Exam prep")

/**
 * Timers, alarms and the study log.
 *
 * Two things live here because they are the same mental job: the focus timer is a
 * stopwatch you run now, an alarm is a stopwatch you set for later. Both feed the
 * same idea of "time spent actually studying".
 */
@Composable
fun FocusScreen(viewModel: FocusViewModel) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val remaining by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val selected by viewModel.selectedMinutes.collectAsStateWithLifecycle()
    val label by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val recent by viewModel.recentSessions.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val todayMinutes by viewModel.todayMinutes.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    var addingAlarm by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { FocusSummaryCard(todayMinutes, totalMinutes) }

        item { TimerCard(phase, remaining, selected, label, viewModel) }

        item {
            AlarmSection(
                alarms = alarms,
                onAdd = { addingAlarm = true },
                onToggle = viewModel::toggleAlarm,
                onDelete = viewModel::deleteAlarm,
                onPreview = viewModel::previewAlarm
            )
        }

        if (totals.isNotEmpty()) {
            item { TotalsCard(totals) }
        }

        if (recent.isNotEmpty()) {
            item { SessionLogHeader(onClear = { confirmClear = true }, recent = recent) }
            items(recent, key = { it.id }) { session ->
                SessionRow(session, onDelete = { viewModel.deleteSession(session.id) })
            }
        }
    }

    if (addingAlarm) {
        AddAlarmDialog(
            onDismiss = { addingAlarm = false },
            onConfirm = { labelText, minuteOfDay, mask, vibrate, sound ->
                viewModel.addAlarm(labelText, minuteOfDay, mask, vibrate, sound)
                addingAlarm = false
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the study log?") },
            text = {
                Text(
                    "This removes all ${recent.size} focus sessions. Your timetable, " +
                        "assignments and alarms are not affected."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    recent.forEach { viewModel.deleteSession(it.id) }
                    confirmClear = false
                }) { Text("Clear log") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun FocusSummaryCard(todayMinutes: Int, totalMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    formatHours(todayMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "All time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    formatHours(totalMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    phase: FocusPhase,
    remaining: Int,
    selected: Int,
    label: String,
    viewModel: FocusViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (phase) {
                    is FocusPhase.Idle -> "Focus timer"
                    is FocusPhase.Running -> phase.label
                    is FocusPhase.Paused -> "${phase.label} (paused)"
                    is FocusPhase.Finished -> "Nice work"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (phase is FocusPhase.Finished) {
                    "${phase.actualMinutes} min logged"
                } else {
                    formatClock(if (phase is FocusPhase.Paused) phase.remainingSeconds else remaining)
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (phase) {
                    is FocusPhase.Idle -> "Pick a length, then start when you are ready."
                    is FocusPhase.Running -> "Running. Leave the app, it keeps going."
                    is FocusPhase.Paused -> "Paused. Resume when you are back."
                    is FocusPhase.Finished -> "Break for 5 minutes, then go again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            if (phase is FocusPhase.Idle || phase is FocusPhase.Finished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FOCUS_PRESETS.take(3).forEach { (minutes, name) ->
                        FilterChip(
                            selected = selected == minutes && phase is FocusPhase.Idle,
                            onClick = { viewModel.selectPreset(minutes) },
                            label = { Text("$minutes") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_LABELS.take(3).forEach { option ->
                        AssistChip(
                            onClick = { viewModel.setLabel(option) },
                            label = { Text(option) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (phase is FocusPhase.Finished) viewModel.discard()
                        viewModel.start(selected, label)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (phase is FocusPhase.Finished) "Start another" else "Start $selected minutes")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (phase is FocusPhase.Running) {
                        FilledTonalButton(
                            onClick = { viewModel.pause() },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause")
                            Spacer(Modifier.size(8.dp))
                            Text("Pause")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.resume() },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume")
                            Spacer(Modifier.size(8.dp))
                            Text("Resume")
                        }
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop and log")
                        Spacer(Modifier.size(8.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSection(
    alarms: List<DailyAlarm>,
    onAdd: () -> Unit,
    onToggle: (DailyAlarm) -> Unit,
    onDelete: (DailyAlarm) -> Unit,
    onPreview: (DailyAlarm) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wake-up alarms", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ring even in silent mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add an alarm")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (alarms.isEmpty()) {
                Text(
                    "No alarms yet. Add one for a lab session, a library slot, or an early class.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                alarms.forEachIndexed { index, alarm ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    AlarmRow(alarm, onToggle, onDelete, onPreview)
                }
            }

            if (!DailyAlarms.canScheduleExact(androidx.compose.ui.platform.LocalContext.current)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Exact alarm access is off, so alarms may arrive a few minutes late. " +
                        "Allow it in Android settings for on-time wake-ups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: DailyAlarm,
    onToggle: (DailyAlarm) -> Unit,
    onDelete: (DailyAlarm) -> Unit,
    onPreview: (DailyAlarm) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (alarm.enabled) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                contentDescription = null,
                tint = if (alarm.enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    DailyAlarms.formatTime(alarm.minuteOfDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    alarm.label.ifBlank { "Alarm" } + " · " + DailyAlarms.daySummary(alarm.daysMask),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onPreview(alarm) }) { Text("Test") }
            IconButton(onClick = { onDelete(alarm) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete alarm")
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Boolean, Boolean) -> Unit
) {
    var label by remember { mutableStateOf("Wake up") }
    var daysMask by remember { mutableStateOf(DailyAlarm.WEEKDAYS) }
    var vibrate by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(true) }
    val timeState = rememberTimePickerState(initialHour = 6, initialMinute = 30, is24Hour = false)
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New alarm") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayNames.forEachIndexed { index, name ->
                        val day = index + 1
                        FilterChip(
                            selected = DailyAlarm.hasDay(daysMask, day),
                            onClick = {
                                daysMask = if (DailyAlarm.hasDay(daysMask, day)) {
                                    daysMask and (1 shl (day - 1)).inv()
                                } else {
                                    daysMask or (1 shl (day - 1))
                                }
                            },
                            label = { Text(name) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = sound, onCheckedChange = { sound = it })
                    Spacer(Modifier.size(8.dp))
                    Text("Sound", modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    Spacer(Modifier.size(8.dp))
                    Text("Vibrate", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(label, timeState.hour * 60 + timeState.minute, daysMask, vibrate, sound)
            }) { Text("Save alarm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TotalsCard(totals: List<com.saintchigos.studyhub.data.FocusTotal>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Where your time went", style = MaterialTheme.typography.titleMedium)
            Text(
                "All focus sessions, grouped by what you were studying",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            totals.take(6).forEach { total ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        total.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        formatHours(total.minutes) + " · " + total.sessions + " sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionLogHeader(onClear: () -> Unit, recent: List<FocusSession>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Study log", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClear) { Text("Clear ${recent.size}") }
    }
}

@Composable
private fun SessionRow(session: FocusSession, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    relativeTime(session.startedAt) + " · " +
                        session.actualMinutes + " of " + session.plannedMinutes + " min" +
                        if (session.completed) " · finished" else " · stopped early",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete session")
            }
        }
    }
}

private fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}

private fun formatHours(minutes: Int): String =
    if (minutes < 60) "$minutes min"
    else {
        val hours = minutes / 60
        val rest = minutes % 60
        if (rest == 0) "${hours}h" else "${hours}h ${rest}m"
    }

private fun relativeTime(startedAt: Long): String {
    val diff = System.currentTimeMillis() - startedAt
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
