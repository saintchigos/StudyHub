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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.CourseAvatar
import com.saintchigos.studyhub.ui.components.CourseTag
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.StatCard
import com.saintchigos.studyhub.ui.components.courseColor
import com.saintchigos.studyhub.ui.components.VerticalDivider
import com.saintchigos.studyhub.ui.theme.SuccessGreen
import com.saintchigos.studyhub.ui.theme.WarningAmber
import com.saintchigos.studyhub.util.TimeUtil

@Composable
fun DashboardScreen(viewModel: StudyHubViewModel) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    var tick by remember { mutableLongStateOf(TimeUtil.toEpochMillis(TimeUtil.now())) }
    val sessions by viewModel.sessionsForSelectedDay.collectAsStateWithLifecycle()

    val open = assignments.filter { !it.isDone }
    val overdue = open.filter { it.dueAt < tick }
    val doneToday = assignments.count { it.isDone }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "StudyHub",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = TimeUtil.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Open tasks",
                    value = open.size.toString(),
                    modifier = Modifier.weight(1f),
                    accent = if (overdue.isNotEmpty()) WarningAmber else MaterialTheme.colorScheme.primary
                )
                StatCard(
                    label = "Overdue",
                    value = overdue.size.toString(),
                    modifier = Modifier.weight(1f),
                    accent = if (overdue.isNotEmpty()) WarningAmber else SuccessGreen
                )
                StatCard(
                    label = "Courses",
                    value = courses.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val nextExam = exams.firstOrNull()
        if (nextExam != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = courseColor(nextExam.colorIndex).copy(alpha = 0.14f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = courseColor(nextExam.colorIndex),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next exam",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = nextExam.courseCode,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = TimeUtil.relativeLabel(nextExam.startsAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = courseColor(nextExam.colorIndex),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        VerticalDivider()
                        Spacer(Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = TimeUtil.formatDate(nextExam.startsAt),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = TimeUtil.formatTime(nextExam.startsAt),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Today's classes",
                trailing = {
                    Text(
                        text = "${sessions.size} session${if (sessions.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        if (sessions.isEmpty()) {
            item {
                EmptyState(
                    title = "No classes today",
                    subtitle = "Enjoy the break, or add a class from the Courses tab."
                )
            }
        } else {
            items(sessions, key = { it.sessionId }) { session ->
                val startsIn = TimeUtil.minutesUntilSession(session.dayOfWeek, session.startMinute)
                val color = courseColor(session.colorIndex)
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
                        Surface(
                            modifier = Modifier
                                .width(4.dp)
                                .height(42.dp),
                            color = color,
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.courseName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    append(TimeUtil.formatClock(session.startMinute))
                                    append(" - ")
                                    append(TimeUtil.formatClock(session.endMinute))
                                    if (session.room.isNotBlank()) {
                                        append("  ·  ")
                                        append(session.room)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CourseAvatar(
                            colorIndex = session.colorIndex,
                            code = session.courseCode
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(
                title = "Due soon",
                trailing = {
                    Text(
                        text = "done: $doneToday",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        val upcoming = assignments.sortedWith(compareBy({ it.isDone }, { it.dueAt })).take(6)
        if (upcoming.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing due",
                    subtitle = "You have cleared your task list. Well done."
                )
            }
        } else {
            items(upcoming, key = { it.id }) { item ->
                val isOverdue = !item.isDone && item.dueAt < tick
                val accent = when {
                    item.isDone -> SuccessGreen
                    isOverdue -> MaterialTheme.colorScheme.error
                    TimeUtil.daysUntil(item.dueAt) <= 1 -> WarningAmber
                    else -> MaterialTheme.colorScheme.primary
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isDone,
                            onCheckedChange = { viewModel.toggleAssignmentDone(item.id, item.isDone) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CourseTag(code = item.courseCode, colorIndex = item.colorIndex)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = TimeUtil.relativeLabel(item.dueAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Icon(
                            imageVector = if (item.isDone) Icons.Filled.CheckCircle
                            else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (exams.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "Exam schedule")
            }
            items(exams.take(4), key = { it.id }) { exam ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Event,
                        contentDescription = null,
                        tint = courseColor(exam.colorIndex),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${exam.courseCode} · ${exam.title}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = buildString {
                                append(TimeUtil.formatDate(exam.startsAt))
                                append(" at ")
                                append(TimeUtil.formatTime(exam.startsAt))
                                if (exam.room.isNotBlank()) {
                                    append(" · ")
                                    append(exam.room)
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = TimeUtil.relativeLabel(exam.startsAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = courseColor(exam.colorIndex),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}