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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.saintchigos.studyhub.ui.components.DateTimeFields
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.ScreenHeader
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.SessionDialog
import com.saintchigos.studyhub.util.TimeUtil
import java.time.LocalTime

@Composable
fun CoursesScreen(viewModel: StudyHubViewModel) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var showCourseDialog by remember { mutableStateOf(false) }
    var sessionCourseId by remember { mutableStateOf<Long?>(null) }
    var examCourseId by remember { mutableStateOf<Long?>(null) }
    var assignmentCourseId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Courses",
            subtitle = "${courses.size} registered",
            trailing = {
                Button(onClick = { showCourseDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        )

        if (courses.isEmpty()) {
            EmptyState(
                title = "No courses yet",
                subtitle = "Pick your programme in Settings to fill these in automatically, or add a course by hand.",
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
                                    text = "${course.code} Â· ${course.credits} credits",
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
        SessionDialog(
            courses = courses,
            initial = null,
            preSelectCourseId = id,
            onDismiss = { sessionCourseId = null },
            onSave = { courseId, day, start, end, room ->
                viewModel.addSession(courseId, day, start, end, room)
                sessionCourseId = null
            }
        )
    }

    assignmentCourseId?.let { id ->
        val course = courses.firstOrNull { it.id == id }
        if (course != null) {
            AddAssignmentDialog(
                courseLabel = "${course.code} Â· ${course.name}",
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
                courseLabel = "${course.code} Â· ${course.name}",
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
private fun AddAssignmentDialog(
    courseLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int) -> Unit
) {
    val today = remember { TimeUtil.now().toLocalDate() }
    var dueDate by remember { mutableStateOf(today.plusDays(3)) }
    var dueTime by remember { mutableStateOf(LocalTime.of(23, 59)) }
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }
    var use24h by remember { mutableStateOf(true) }

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
                Spacer(Modifier.height(14.dp))
                DateTimeFields(
                    date = dueDate,
                    time = dueTime,
                    onDateChange = { dueDate = it },
                    onTimeChange = { dueTime = it },
                    earliest = today,
                    dateLabel = "Due date",
                    is24Hour = use24h
                )
                Spacer(Modifier.height(14.dp))
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in listOf(0 to "Low", 1 to "Normal", 2 to "High")) {
                        FilterChip(
                            selected = priority == p.first,
                            onClick = { priority = p.first },
                            label = { Text(p.second) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = use24h, onCheckedChange = { use24h = it })
                    Spacer(Modifier.width(10.dp))
                    Text("24-hour time", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val due = dueDate.atTime(dueTime)
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
    val today = remember { TimeUtil.now().toLocalDate() }
    var examDate by remember { mutableStateOf(today.plusDays(14)) }
    var examTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var title by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("120") }
    var use24h by remember { mutableStateOf(true) }

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
                Spacer(Modifier.height(14.dp))
                DateTimeFields(
                    date = examDate,
                    time = examTime,
                    onDateChange = { examDate = it },
                    onTimeChange = { examTime = it },
                    earliest = today,
                    dateLabel = "Exam date",
                    is24Hour = use24h
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(
                        checked = use24h,
                        onCheckedChange = { use24h = it }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "24-hour time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                    val start = examDate.atTime(examTime)
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