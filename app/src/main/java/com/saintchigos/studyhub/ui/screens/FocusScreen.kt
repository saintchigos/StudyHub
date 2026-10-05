package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.DailyAlarm
import com.saintchigos.studyhub.data.FocusSession
import com.saintchigos.studyhub.reminder.DailyAlarms
import com.saintchigos.studyhub.ui.FocusPhase
import com.saintchigos.studyhub.ui.FocusViewModel

/** Durations offered up front. Any length can be dialled in on the slider. */
private val DURATION_CHOICES = listOf(15, 25, 45, 60)

private val SUBJECT_CHOICES = listOf("Deep work", "Revision", "Homework", "Reading", "Exam prep")

/**
 * Timers, alarms and the study log.
 *
 * Two things live here because they are the same mental job: the focus timer is a
 * stopwatch you run now, an alarm is a stopwatch you set for later. Both feed the
 * same idea of "time spent actually studying".
 *
 * Laid out one card per job rather than one long scroll, so the timer is visible
 * without scrolling past the alarm list on a small screen.
 */
@Composable
fun FocusScreen(viewModel: FocusViewModel) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    // While idle the dial mirrors the chosen length, so it falls back to the preset
    // rather than to a stale zero left over from the last run.
    val remaining by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val selected by viewModel.selectedMinutes.collectAsStateWithLifecycle()
    val label by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val recent by viewModel.recentSessions.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val todayMinutes by viewModel.todayMinutes.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()

    var editingAlarm by remember { mutableStateOf<DailyAlarm?>(null) }
    var addingAlarm by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FocusSummaryCard(
                todayMinutes = todayMinutes,
                totalMinutes = totalMinutes,
                goalMinutes = dailyGoal,
                onEditGoal = { editingGoal = true }
            )
        }

        item { TimerCard(phase, remaining, selected, label, viewModel) }

        item {
            AlarmSection(
                alarms = alarms,
                onAdd = { addingAlarm = true },
                onToggle = viewModel::toggleAlarm,
                onDelete = viewModel::deleteAlarm,
                onPreview = viewModel::previewAlarm,
                onEdit = { editingAlarm = it }
            )
        }

        if (totals.isNotEmpty()) {
            item { TotalsCard(totals) }
        }

        if (recent.isNotEmpty()) {
            item { SessionLogHeader(onClear = { confirmClear = true }, count = sessionCount) }
            items(recent, key = { it.id }) { session ->
                SessionRow(session, onDelete = { viewModel.deleteSession(session.id) })
            }
        }
    }

    if (editingGoal) {
        GoalDialog(
            current = dailyGoal,
            onDismiss = { editingGoal = false },
            onSave = {
                viewModel.setDailyGoal(it)
                editingGoal = false
            }
        )
    }

    if (addingAlarm || editingAlarm != null) {
        AlarmEditorDialog(
            existing = editingAlarm,
            onDismiss = {
                addingAlarm = false
                editingAlarm = null
            },
            onSave = { alarmLabel, minuteOfDay, mask, vibrate, sound ->
                val existing = editingAlarm
                if (existing == null) {
                    viewModel.addAlarm(alarmLabel, minuteOfDay, mask, vibrate, sound)
                } else {
                    viewModel.updateAlarm(
                        existing.copy(
                            label = alarmLabel,
                            minuteOfDay = minuteOfDay,
                            daysMask = mask,
                            vibrate = vibrate,
                            sound = sound
                        )
                    )
                }
                addingAlarm = false
                editingAlarm = null
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the study log?") },
            text = {
                Text(
                    "This removes all $sessionCount focus sessions. Your timetable, " +
                        "assignments and alarms are not affected."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSessions()
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
private fun FocusSummaryCard(
    todayMinutes: Int,
    totalMinutes: Int,
    goalMinutes: Int,
    onEditGoal: () -> Unit
) {
    val progress = if (goalMinutes <= 0) {
        0f
    } else {
        (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
    }
    val goalMet = todayMinutes >= goalMinutes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                StatColumn("Today", formatHours(todayMinutes), Modifier.weight(1f))
                StatColumn("All time", formatHours(totalMinutes), Modifier.weight(1f))
                TextButton(onClick = onEditGoal) { Text("Goal") }
            }

            Spacer(Modifier.height(12.dp))

            // A bar rather than a second ring: the timer already owns the ring, and
            // two rings on one screen compete for the same glance.
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (goalMet) {
                    "Daily goal reached. ${formatHours(todayMinutes - goalMinutes)} over " +
                        "${formatHours(goalMinutes)}."
                } else {
                    "${formatHours(goalMinutes - todayMinutes)} to your ${formatHours(goalMinutes)} goal."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val options = listOf(30, 60, 120, 180, 240, 360)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily focus goal") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "How much real study time do you want to hit each day? " +
                        "Timers that you stop early still count only what you focused.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                // Wrapped rows rather than one row, so six options still fit on a
                // narrow phone without the chips shrinking below their label.
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { option ->
                            FilterChip(
                                selected = current == option,
                                onClick = { onSave(option) },
                                label = { Text(formatHours(option)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
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
    val running = phase is FocusPhase.Running || phase is FocusPhase.Paused
    val displaySeconds = when (phase) {
        is FocusPhase.Paused -> phase.remainingSeconds
        is FocusPhase.Running -> remaining
        else -> selected * 60
    }
    val totalSeconds = when (phase) {
        is FocusPhase.Running -> phase.plannedMinutes * 60
        is FocusPhase.Paused -> phase.plannedMinutes * 60
        else -> selected * 60
    }
    val progress = if (totalSeconds <= 0) {
        0f
    } else {
        (displaySeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (phase) {
                    is FocusPhase.Idle -> "Focus timer"
                    is FocusPhase.Running -> phase.label
                    is FocusPhase.Paused -> "${phase.label}, paused"
                    is FocusPhase.Finished -> "Block done"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // A ring instead of a bare number: the remaining arc is the only place
            // that shows progress at a glance without reading the digits.
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                if (running || phase is FocusPhase.Finished) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 10.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (phase) {
                            is FocusPhase.Finished -> "${phase.actualMinutes}"
                            else -> formatClock(displaySeconds)
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (phase is FocusPhase.Finished) "minutes logged" else "remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = when (phase) {
                    is FocusPhase.Idle -> "Pick a length, then start when you are ready."
                    is FocusPhase.Running -> "Running. Leave the app, it keeps going."
                    is FocusPhase.Paused -> "Paused. Resume when you are back."
                    is FocusPhase.Finished -> "Take a short break, then go again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            if (!running) {
                DurationControls(selected, viewModel)

                Spacer(Modifier.height(12.dp))
                SubjectControls(label, viewModel)

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (phase is FocusPhase.Finished) viewModel.discard()
                        viewModel.start(selected, label)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (phase is FocusPhase.Finished) "Start another" else "Start $selected min")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (phase is FocusPhase.Paused) viewModel.resume() else viewModel.pause() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (phase is FocusPhase.Paused) {
                                Icons.Filled.PlayArrow
                            } else {
                                Icons.Filled.Pause
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (phase is FocusPhase.Paused) "Resume" else "Pause")
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Finish now")
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationControls(selected: Int, viewModel: FocusViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "How long",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DURATION_CHOICES.forEach { minutes ->
                FilterChip(
                    selected = selected == minutes,
                    onClick = { viewModel.selectPreset(minutes) },
                    label = { Text("$minutes min") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // A slider catches the lengths the presets miss, without a keyboard dialog.
        Slider(
            value = selected.toFloat(),
            onValueChange = { viewModel.selectPreset(it.toInt()) },
            valueRange = 5f..120f,
            steps = 22,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$selected minutes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectControls(label: String, viewModel: FocusViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Studying",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUBJECT_CHOICES.take(3).forEach { option ->
                FilterChip(
                    selected = label == option,
                    onClick = { viewModel.setLabel(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUBJECT_CHOICES.drop(3).forEach { option ->
                FilterChip(
                    selected = label == option,
                    onClick = { viewModel.setLabel(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlarmSection(
    alarms: List<DailyAlarm>,
    onAdd: () -> Unit,
    onToggle: (DailyAlarm) -> Unit,
    onDelete: (DailyAlarm) -> Unit,
    onPreview: (DailyAlarm) -> Unit,
    onEdit: (DailyAlarm) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exact = DailyAlarms.canScheduleExact(context)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wake-up alarms", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Rings in silent mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAdd,
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Add")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (alarms.isEmpty()) {
                Text(
                    "No alarms yet. Add one for a lab, an early class, or a library slot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                alarms.forEachIndexed { index, alarm ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    AlarmRow(alarm, onToggle, onDelete, onPreview, onEdit)
                }
            }

            if (!exact) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Exact alarm access is off, so alarms may arrive a few minutes " +
                            "late. Allow it in Android settings for on-time wake-ups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * One alarm as a tappable card.
 *
 * Previously this was a single row with a Test button, a delete icon and a switch
 * competing for the same 360dp of width, so the label truncated and none of the
 * targets were comfortably tappable. The row is now the switch, and the secondary
 * actions sit on their own full-width line underneath.
 */
@Composable
private fun AlarmRow(
    alarm: DailyAlarm,
    onToggle: (DailyAlarm) -> Unit,
    onDelete: (DailyAlarm) -> Unit,
    onPreview: (DailyAlarm) -> Unit,
    onEdit: (DailyAlarm) -> Unit
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        alarm.label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm) })
            }

            Spacer(Modifier.height(8.dp))

            // Parenthesised because these are alternatives, not concatenations:
            // without the grouping the vibrate flag would only ever appear on
            // silent alarms.
            val extras = buildList {
                add(if (alarm.sound) "sound" else "silent")
                if (alarm.vibrate) add("vibrate")
            }
            // A plain hyphen rather than a middot: the middot does not render in the
                // default typeface on this OEM build and showed as two boxes.
            Text(
                DailyAlarms.daySummary(alarm.daysMask) + " - " + extras.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onPreview(alarm) }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Test")
                }
                TextButton(onClick = { onEdit(alarm) }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit")
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this alarm?") },
            text = { Text("${alarm.label.ifBlank { "Alarm" }} at ${DailyAlarms.formatTime(alarm.minuteOfDay)}") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(alarm)
                    confirmingDelete = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Keep") }
            }
        )
    }
}

/**
 * Creates or edits an alarm.
 *
 * [existing] is null when adding, so the same dialog handles both and the edit
 * path starts from the current values instead of silently resetting them.
 *
 * The time is picked in a second dialog rather than inline. Material3's clock dial
 * is roughly 500dp tall, and dropping that into this dialog on a 800dp phone pushed
 * the Sound and Vibrate rows and then the buttons clean off the bottom of the
 * screen. Splitting it keeps every control reachable on a small device.
 */
@Composable
private fun AlarmEditorDialog(
    existing: DailyAlarm?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Boolean, Boolean) -> Unit
) {
    val initialMinuteOfDay = existing?.minuteOfDay ?: (6 * 60 + 30)
    var label by remember { mutableStateOf(existing?.label ?: "Wake up") }
    var daysMask by remember { mutableStateOf(existing?.daysMask ?: DailyAlarm.WEEKDAYS) }
    var vibrate by remember { mutableStateOf(existing?.vibrate ?: true) }
    var sound by remember { mutableStateOf(existing?.sound ?: true) }
    var minuteOfDay by remember { mutableStateOf(initialMinuteOfDay) }
    var pickingTime by remember { mutableStateOf(false) }

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Guards the case where every chip is unticked, which would schedule an alarm
    // that never repeats and looks like it works while never ringing again.
    val noDaysPicked = daysMask == 0
    val chosenDays = dayLabels.mapIndexedNotNull { index, name ->
        if (DailyAlarm.hasDay(daysMask, index + 1)) name else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New alarm" else "Edit alarm") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // The whole row is the target, so the student does not have to hit
                // the small clock glyph precisely.
                Card(
                    onClick = { pickingTime = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            DailyAlarms.formatTime(minuteOfDay),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Repeat", style = MaterialTheme.typography.labelLarge)
                    Row {
                        TextButton(onClick = { daysMask = DailyAlarm.WEEKDAYS }) { Text("Weekdays") }
                        TextButton(onClick = { daysMask = DailyAlarm.ALL_DAYS }) { Text("Every day") }
                    }
                }

                // Full names across two rows instead of single letters on one, which
                // had two T's and two S's and read as noise at that size.
                dayLabels.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { name ->
                            val day = dayLabels.indexOf(name) + 1
                            FilterChip(
                                selected = DailyAlarm.hasDay(daysMask, day),
                                onClick = {
                                    daysMask = if (DailyAlarm.hasDay(daysMask, day)) {
                                        daysMask and (1 shl (day - 1)).inv()
                                    } else {
                                        daysMask or (1 shl (day - 1))
                                    }
                                },
                                label = { Text(name, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Text(
                    text = if (noDaysPicked) {
                        "Pick at least one day"
                    } else {
                        chosenDays.joinToString(" ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (noDaysPicked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { sound = !sound },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = sound, onCheckedChange = { sound = it })
                    Spacer(Modifier.size(12.dp))
                    Text("Sound", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        if (sound) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { vibrate = !vibrate },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    Spacer(Modifier.size(12.dp))
                    Text("Vibrate", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        if (vibrate) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label.trim(), minuteOfDay, daysMask, vibrate, sound) },
                enabled = !noDaysPicked,
                modifier = Modifier.height(48.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Cancel") }
        }
    )

    if (pickingTime) {
        AlarmTimeDialog(
            initialMinuteOfDay = minuteOfDay,
            onDismiss = { pickingTime = false },
            onConfirm = {
                minuteOfDay = it
                pickingTime = false
            }
        )
    }
}

/**
 * The clock on its own, with no other controls competing for vertical space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimeDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alarm time") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timeState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timeState.hour * 60 + timeState.minute) },
                modifier = Modifier.height(48.dp)
            ) { Text("Set time") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Back") }
        }
    )
}

@Composable
private fun TotalsCard(totals: List<com.saintchigos.studyhub.data.FocusTotal>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Where your time went", style = MaterialTheme.typography.titleMedium)
            Text(
                "All focus sessions, grouped by subject",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            totals.take(6).forEach { total ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        total.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        formatHours(total.minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        " · ${total.sessions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionLogHeader(onClear: () -> Unit, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Study log", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClear) { Text("Clear $count") }
    }
}

@Composable
private fun SessionRow(session: FocusSession, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = if (session.completed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                relativeTime(session.startedAt) + " · " + session.actualMinutes + " min" +
                    if (session.completed) "" else " of ${session.plannedMinutes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete session",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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