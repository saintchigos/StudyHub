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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.CourseTag
import com.saintchigos.studyhub.ui.components.EmptyState
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.theme.SuccessGreen
import com.saintchigos.studyhub.ui.theme.WarningAmber
import com.saintchigos.studyhub.util.TimeUtil

private enum class TaskFilter(val label: String) {
    ALL("All"),
    OPEN("Open"),
    DONE("Done")
}

@Composable
fun AssignmentsScreen(viewModel: StudyHubViewModel) {
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val now = TimeUtil.toEpochMillis(TimeUtil.now())
    var filter by remember { mutableStateOf(TaskFilter.ALL) }

    val visible = when (filter) {
        TaskFilter.ALL -> assignments
        TaskFilter.OPEN -> assignments.filter { !it.isDone }
        TaskFilter.DONE -> assignments.filter { it.isDone }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Text("Assignments", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${assignments.count { !it.isDone }} open of ${assignments.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) }
                )
            }
        }

        if (visible.isEmpty()) {
            EmptyState(
                title = "Nothing here",
                subtitle = when (filter) {
                    TaskFilter.DONE -> "No completed assignments yet."
                    TaskFilter.OPEN -> "You have finished everything due. Nice work."
                    TaskFilter.ALL -> "Add an assignment to get started."
                },
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
                item { SectionHeader(title = "By deadline") }
                items(visible, key = { it.id }) { item ->
                    val isOverdue = !item.isDone && item.dueAt < now
                    val accent = when {
                        item.isDone -> SuccessGreen
                        isOverdue -> MaterialTheme.colorScheme.error
                        TimeUtil.daysUntil(item.dueAt) <= 1 -> WarningAmber
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val priorityLabel = when (item.priority) {
                        2 -> "High"
                        0 -> "Low"
                        else -> "Normal"
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
                                onCheckedChange = {
                                    viewModel.toggleAssignmentDone(item.id, item.isDone)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.isDone)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CourseTag(code = item.courseCode, colorIndex = item.colorIndex)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "$priorityLabel · ${TimeUtil.formatDate(item.dueAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = TimeUtil.relativeLabel(item.dueAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (item.isDone) Icons.Filled.CheckCircle
                                else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}