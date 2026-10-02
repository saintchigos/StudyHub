package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.CourseAvatar
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.util.TimeUtil

@Composable
fun CoursesScreen(viewModel: StudyHubViewModel) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var showCourseDialog by remember { mutableStateOf(false) }
    var sessionCourseId by remember { mutableStateOf<Long?>(null) }
    var examCourseId by remember { mutableStateOf<Long?>(null) }
    var assignmentCourseId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Courses", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "${courses.size} registered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { showCourseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }

        if (courses.isEmpty()) {
            EmptyState(
                title = "No courses yet",
                subtitle = "Add a course, then attach classes, assignments and exams to it.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SectionHeader(title = "Enrolled") }
                items(courses, key = { it.id }) { course ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CourseAvatar(
                                colorIndex = course.colorIndex,
                                code = course.code,
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(course.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${course.code} · ${course.credits} credits",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = { sessionCourseId = course.id }) { Text("Class") }
                            TextButton(onClick = { assignmentCourseId = course.id }) { Text("Assignment") }
                            TextButton(onClick = { examCourseId = course.id }) { Text("Exam") }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteCourse(course) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete ${course.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCourseDialog) {
        AddCourseDialog(
            onDismiss = { showCourseDialog = false },
            onConfirm = { name, code, credits ->
                viewModel.addCourse(name, code, credits)
                showCourseDialog = false
            }
        )
    }

    sessionCourseId?.let { id ->
        val course = courses.firstOrNull { it.id == id }
        if (course != null) {
            AddSessionDialog(
                courseLabel = "${course.code} · ${course.name}",
                onDismiss = { sessionCourseId = null },
                onConfirm = { day, start, end, room ->
                    viewModel.addSession(id, day, start, end, room)
                    sessionCourseId = null
                }
            )
        }
    }

    assignmentCourseId?.let { id ->
        val course = courses.firstOrNull { it.id == id }
        if (course != null) {
            AddAssignmentDialog(
                courseLabel = "${course.code} · ${course.name}",
                onDismiss = { assignmentCourseId = null },
                onConfirm = { title, dueAt, priority ->
                    viewModel.addAssignment(id, title, dueAt, priority)
                    assignmentCourseId = null
                }
            )
        }
    }

    examCourseId?.let { id ->
        val course = courses.firstOrNull { it.id == id }
        if (course != null) {
            AddExamDialog(
                courseLabel = "${course.code} · ${course.name}",
                onDismiss = { examCourseId = null },
                onConfirm = { title, startsAt, duration, room, notes ->
                    viewModel.addExam(id, title, startsAt, duration, room, notes)
                    examCourseId = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New course") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it.filter(Char::isDigit).take(2) },
                    label = { Text("Credits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, code, credits.toIntOrNull() ?: 3) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSessionDialog(
    courseLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, String) -> Unit
) {
    var day by remember { mutableStateOf(1) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("") }
    var dayOpen by remember { mutableStateOf(false) }

    fun parseTime(t: String): Int? {
        val parts = t.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    val valid = parseTime(start) != null && parseTime(end) != null &&
        (parseTime(start) ?: 0) < (parseTime(end) ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add class") },
        text = {
            Column {
                Text(
                    courseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = dayOpen,
                    onExpandedChange = { dayOpen = it }
                ) {
                    OutlinedTextField(
                        value = TimeUtil.dayLabel(day),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayOpen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dayOpen,
                        onDismissRequest = { dayOpen = false }
                    ) {
                        for (d in 1..7) {
                            DropdownMenuItem(
                                text = { Text(TimeUtil.dayLabel(d)) },
                                onClick = { day = d; dayOpen = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!valid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter times as HH:mm and make sure end is after start.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(day, parseTime(start)!!, parseTime(end)!!, room) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssignmentDialog(
    courseLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }
    var daysOffset by remember { mutableStateOf(3) }
    var hour by remember { mutableStateOf(23) }
    var minute by remember { mutableStateOf(59) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New assignment") },
        text = {
            Column {
                Text(
                    courseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Due", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in listOf(1, 3, 7, 14)) {
                        androidx.compose.material3.FilterChip(
                            selected = daysOffset == d,
                            onClick = { daysOffset = d },
                            label = { Text("${d}d") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in listOf(0 to "Low", 1 to "Normal", 2 to "High")) {
                        androidx.compose.material3.FilterChip(
                            selected = priority == p.first,
                            onClick = { priority = p.first },
                            label = { Text(p.second) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val due = TimeUtil.now()
                        .toLocalDate()
                        .plusDays(daysOffset.toLong())
                        .atTime(hour, minute)
                    onConfirm(title, TimeUtil.toEpochMillis(due), priority)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamDialog(
    courseLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var daysOffset by remember { mutableStateOf(14) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf("120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exam") },
        text = {
            Column {
                Text(
                    courseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assessment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Date", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in listOf(7, 14, 21, 30)) {
                        androidx.compose.material3.FilterChip(
                            selected = daysOffset == d,
                            onClick = { daysOffset = d },
                            label = { Text("${d}d") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Start time", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (h in listOf(8, 9, 10, 13, 14)) {
                        androidx.compose.material3.FilterChip(
                            selected = hour == h,
                            onClick = { hour = h },
                            label = { Text("${h}:00") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Venue (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val start = TimeUtil.now()
                        .toLocalDate()
                        .plusDays(daysOffset.toLong())
                        .atTime(hour, minute)
                    onConfirm(
                        title,
                        TimeUtil.toEpochMillis(start),
                        duration.toIntOrNull() ?: 120,
                        room,
                        notes
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}