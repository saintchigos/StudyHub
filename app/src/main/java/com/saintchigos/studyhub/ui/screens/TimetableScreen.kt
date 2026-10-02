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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.SessionWithCourse
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.CourseAvatar
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.SessionDialog
import com.saintchigos.studyhub.util.TimeUtil

@Composable
fun TimetableScreen(viewModel: StudyHubViewModel) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val sessions by viewModel.sessionsForSelectedDay.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val todayIso = TimeUtil.now().dayOfWeek.value

    var editing by remember { mutableStateOf<SessionWithCourse?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Text("Timetable", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Tap a class to edit it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (day in 1..7) {
                    val isToday = day == todayIso
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { viewModel.setDay(day) },
                        label = { Text(TimeUtil.shortDayLabel(day)) },
                        leadingIcon = if (isToday) {
                            {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedDay == day)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                }
            }

            if (sessions.isEmpty()) {
                EmptyState(
                    title = "Nothing scheduled",
                    subtitle = "${TimeUtil.dayLabel(selectedDay)} is free. Use the button below to add a class.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = TimeUtil.dayLabel(selectedDay),
                            trailing = {
                                Text(
                                    text = "${sessions.size} session${if (sessions.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                    items(sessions, key = { it.sessionId }) { session ->
                        Card(
                            onClick = { editing = session },
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Text(
                                        text = TimeUtil.formatClock24(session.startMinute),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = TimeUtil.formatClock24(session.endMinute),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.courseName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (session.room.isBlank()) "Room not set"
                                        else "Room ${session.room}",
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
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add class")
        }
    }

    if (adding) {
        SessionDialog(
            courses = courses,
            initial = null,
            onDismiss = { adding = false },
            onSave = { courseId, day, startMinute, endMinute, room ->
                viewModel.addSession(courseId, day, startMinute, endMinute, room)
                adding = false
            }
        )
    }

    editing?.let { session ->
        SessionDialog(
            courses = courses,
            initial = session,
            onDismiss = { editing = null },
            onSave = { courseId, day, startMinute, endMinute, room ->
                viewModel.editSession(
                    session.sessionId, courseId, day, startMinute, endMinute, room
                )
                editing = null
            },
            onDelete = { id ->
                viewModel.deleteSession(id)
                editing = null
            }
        )
    }
}