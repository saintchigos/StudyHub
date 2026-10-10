package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.Course
import com.saintchigos.studyhub.domain.GpaCalculator
import java.util.Locale

/**
 * Tap a grade next to each course to see the GPA it would give.
 *
 * Each tap on the grade button moves to the next letter, which keeps the dialog to one
 * control per course instead of a dropdown per row. Nothing is saved: it is a "what if".
 */
@Composable
fun GpaDialog(courses: List<Course>, onDismiss: () -> Unit) {
    // Course id to the index of the chosen letter in GpaCalculator.GRADES; absent means "not chosen".
    val chosen = remember { mutableStateMapOf<Long, Int>() }

    val entries = courses.mapNotNull { course ->
        val index = chosen[course.id] ?: return@mapNotNull null
        GpaCalculator.Entry(course.credits, GpaCalculator.GRADES[index].first)
    }
    val gpa = GpaCalculator.gpa(entries)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPA estimator") },
        text = {
            Column {
                Text(
                    text = "Tap a grade to change it. Courses you leave blank are not counted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    courses.forEach { course ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.code.ifBlank { course.name },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${course.credits} credits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val index: Int? = chosen[course.id]
                            FilledTonalButton(
                                onClick = {
                                    // Unset, then A, A-, B+ ... F, then back to unset.
                                    val next = if (index == null) 0 else index + 1
                                    if (next >= GpaCalculator.GRADES.size) {
                                        chosen.remove(course.id)
                                    } else {
                                        chosen[course.id] = next
                                    }
                                }
                            ) {
                                Text(if (index == null) "Grade" else GpaCalculator.GRADES[index].first)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (gpa == null) {
                        "Pick at least one grade."
                    } else {
                        "Estimated GPA: " + String.format(Locale.US, "%.2f", gpa)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
