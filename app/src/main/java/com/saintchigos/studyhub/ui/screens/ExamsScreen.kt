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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.CourseAvatar
import com.saintchigos.studyhub.ui.components.CourseTag
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.ScreenHeader
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.courseColor
import com.saintchigos.studyhub.util.TimeUtil

@Composable
fun ExamsScreen(viewModel: StudyHubViewModel) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Exams",
            subtitle = if (exams.isEmpty()) "Nothing scheduled"
            else "${exams.size} upcoming assessment${if (exams.size == 1) "" else "s"}"
        )

        if (exams.isEmpty()) {
            EmptyState(
                title = "No exams yet",
                subtitle = "Add an exam and StudyHub will count down for you.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SectionHeader(title = "Countdown") }
                items(exams, key = { it.id }) { exam ->
                    val color = courseColor(exam.colorIndex)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = color.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CourseAvatar(colorIndex = exam.colorIndex, code = exam.courseCode)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exam.courseName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = exam.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                CourseTag(code = exam.courseCode, colorIndex = exam.colorIndex)
                            }

                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = color
                            ) {
                                Text(
                                    text = TimeUtil.relativeLabel(exam.startsAt),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.width(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${TimeUtil.formatDate(exam.startsAt)} at " +
                                        TimeUtil.formatTime(exam.startsAt) +
                                        " · ${exam.durationMinutes} min",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (exam.room.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = null,
                                        modifier = Modifier.width(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = exam.room,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (exam.notes.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = exam.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}