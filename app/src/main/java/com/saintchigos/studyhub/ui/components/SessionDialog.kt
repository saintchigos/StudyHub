package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.util.TimeUtil
import java.time.Duration
import java.time.LocalTime

/**
 * Single dialog for both creating and editing a weekly class.
 * Pass initial = null to create, or an existing session to edit it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDialog(
    courses: List<Course>,
    initial: SessionWithCourse?,
    onDismiss: () -> Unit,
    onSave: (courseId: Long, dayOfWeek: Int, startMinute: Int, endMinute: Int, room: String) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    preSelectCourseId: Long? = null
) {
    val startingCourseId = initial?.courseId
        ?: preSelectCourseId
        ?: courses.firstOrNull()?.id
        ?: 0L

    var courseId by remember { mutableStateOf(startingCourseId) }
    var day by remember { mutableStateOf(initial?.dayOfWeek ?: TimeUtil.now().dayOfWeek.value) }
    var start by remember {
        mutableStateOf(
            initial?.let { TimeUtil.minuteOfDayToLocalTime(it.startMinute) } ?: LocalTime.of(8, 0)
        )
    }
    var end by remember {
        mutableStateOf(
            initial?.let { TimeUtil.minuteOfDayToLocalTime(it.endMinute) } ?: LocalTime.of(10, 0)
        )
    }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var courseOpen by remember { mutableStateOf(false) }

    val durationMinutes = Duration.between(start, end).toMinutes()
    val timeValid = durationMinutes > 0
    val selectedCourse = courses.firstOrNull { it.id == courseId }
    val canSave = courseId > 0L && timeValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add class" else "Edit class") },
        text = {
            Column {
                if (initial == null && preSelectCourseId == null) {
                    ExposedDropdownMenuBox(
                        expanded = courseOpen,
                        onExpandedChange = { courseOpen = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse?.let { "${it.code} · ${it.name}" } ?: "No courses yet",
                            onValueChange = {},
                            readOnly = true,
                            enabled = courses.isNotEmpty(),
                            label = { Text("Course") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseOpen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = courseOpen,
                            onDismissRequest = { courseOpen = false }
                        ) {
                            for (c in courses) {
                                DropdownMenuItem(
                                    text = { Text("${c.code} · ${c.name}") },
                                    onClick = { courseId = c.id; courseOpen = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        text = selectedCourse?.let { "${it.code} · ${it.name}" } ?: "Course",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Text("Day", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (d in listOf(1, 2, 3, 4)) {
                            DayChip(day, d) { day = d }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (d in listOf(5, 6, 7)) {
                            DayChip(day, d) { day = d }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimePickerField(
                        time = start,
                        onTimeChange = { start = it },
                        modifier = Modifier.weight(1f),
                        label = "Starts"
                    )
                    TimePickerField(
                        time = end,
                        onTimeChange = { end = it },
                        modifier = Modifier.weight(1f),
                        label = "Ends"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (timeValid)
                        "Length ${durationMinutes / 60}h ${durationMinutes % 60}m" +
                            if (durationMinutes % 60 > 0) "" else ""
                    else "End time must be after the start time",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (timeValid) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        courseId,
                        day,
                        start.hour * 60 + start.minute,
                        end.hour * 60 + end.minute,
                        room
                    )
                }
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial.sessionId) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun DayChip(selectedDay: Int, day: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selectedDay == day,
        onClick = onClick,
        label = { Text(TimeUtil.shortDayLabel(day)) }
    )
}