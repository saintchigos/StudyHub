package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.saintchigos.studyhub.data.ClassSession
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.domain.ScheduleClash
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Add-assignment dialog.
 *
 * Tasks and tests both existed in the database and on Home but had no way to be
 * created from their own screens, so a student had to leave Tasks to add a task.
 * These dialogs put the add button where the work is.
 *
 * Scrolling is deliberate: a keyboard plus a date, a time and a course picker does
 * not fit a 360x800dp screen otherwise, and the Save button ends up below the fold.
 */
@Composable
fun AddAssignmentDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (courseId: Long, title: String, dueAt: Long, priority: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 0L) }
    var priority by remember { mutableStateOf(1) }
    var date by remember { mutableStateOf(TimeUtil.now().toLocalDate().plusDays(1)) }
    var time by remember { mutableStateOf(LocalTime.of(17, 0)) }

    val canSave = title.isNotBlank() && courseId > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New assignment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is due?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CoursePickerField(
                    courses = courses,
                    selectedId = courseId,
                    onSelect = { courseId = it }
                )

                Text(
                    "Priority",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low" to 0, "Normal" to 1, "High" to 2).forEach { (label, value) ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(label) }
                        )
                    }
                }

                DateTimeFields(
                    date = date,
                    time = time,
                    onDateChange = { date = it },
                    onTimeChange = { time = it },
                    earliest = TimeUtil.now().toLocalDate(),
                    dateLabel = "Due date"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        courseId,
                        title,
                        TimeUtil.toEpochMillis(LocalDateTime.of(date, time)),
                        priority
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Add-exam dialog.
 *
 * Same reason as [AddAssignmentDialog]: an exam list with no add button forces the
 * student out of Exams to add a test.
 */
@Composable
fun AddExamDialog(
    courses: List<Course>,
    sessions: List<ClassSession> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (courseId: Long, title: String, startsAt: Long, durationMinutes: Int, room: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: 0L) }
    var room by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(120) }
    var date by remember { mutableStateOf(TimeUtil.now().toLocalDate().plusDays(7)) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }

    val canSave = title.isNotBlank() && courseId > 0

    // Recomputed as the student types rather than once on open, so moving the date
    // or the duration updates the warning immediately.
    val clash = remember(date, time, duration, sessions) {
        val startsAt = TimeUtil.toEpochMillis(LocalDateTime.of(date, time))
        sessions.firstOrNull { ScheduleClash.examVsClass(startsAt, duration, it) }
            ?.let { ScheduleClash.describe(it, startsAt, duration, ZoneId.systemDefault()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exam or test") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam or test name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CoursePickerField(
                    courses = courses,
                    selectedId = courseId,
                    onSelect = { courseId = it }
                )

                DateTimeFields(
                    date = date,
                    time = time,
                    onDateChange = { date = it },
                    onTimeChange = { time = it },
                    earliest = TimeUtil.now().toLocalDate(),
                    dateLabel = "Starts"
                )

                Text("Duration", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 120, 180).forEach { minutes ->
                        FilterChip(
                            selected = duration == minutes,
                            onClick = { duration = minutes },
                            label = { Text("${minutes / 60}h") }
                        )
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Venue (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // A warning, not a block. Exams clash with lectures constantly, and
                // sometimes the only answer really is to sit both. Stopping the
                // student saving would just push them to write it down elsewhere.
                if (clash != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Column {
                                Text(
                                    "Clashes with a class",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    clash.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "You can still save it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        courseId,
                        title,
                        TimeUtil.toEpochMillis(LocalDateTime.of(date, time)),
                        duration,
                        room,
                        notes
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Course dropdown shared by both dialogs.
 *
 * A course is required by the database, so the student must be able to pick one
 * without leaving the dialog.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CoursePickerField(
    courses: List<Course>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = courses.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.code ?: if (courses.isEmpty()) "No courses yet" else "Choose a course",
            onValueChange = {},
            readOnly = true,
            label = { Text("Course") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text("${course.code} · ${course.name}") },
                    onClick = {
                        onSelect(course.id)
                        expanded = false
                    }
                )
            }
        }
    }
}