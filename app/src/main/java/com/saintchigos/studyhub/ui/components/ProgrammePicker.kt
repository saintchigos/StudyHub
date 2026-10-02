package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.PlanWithProgramme

/** Years offered by the picker. Programmes longer than six years are rare. */
private val YEARS = listOf(1, 2, 3, 4, 5, 6)
private val SEMESTERS = listOf("A", "B")

fun planLabel(plan: PlanWithProgramme): String = "${plan.programmeName} · Year ${plan.year}, Semester ${plan.semester}"

/** Year and semester chips shared by the add-major and add-plan dialogs. */
@Composable
fun YearSemesterRow(
    year: Int,
    semester: String,
    onYearChange: (Int) -> Unit,
    onSemesterChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Year of study", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YEARS.forEach { option ->
                FilterChip(
                    selected = year == option,
                    onClick = { onYearChange(option) },
                    label = { Text("Y$option") }
                )
            }
        }
        Text("Semester", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SEMESTERS.forEach { option ->
                FilterChip(
                    selected = semester == option,
                    onClick = { onSemesterChange(option) },
                    label = { Text("Semester $option") }
                )
            }
        }
    }
}

/**
 * One selectable programme offering. Applying never deletes existing data, it only
 * adds whatever is missing.
 */
@Composable
fun ProgrammePlanCard(
    plan: PlanWithProgramme,
    stats: Pair<Int, Int>,
    onApply: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (courseCount, sessionCount) = stats
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.programmeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Year ${plan.year} · Semester ${plan.semester}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (plan.isCustom) {
                    AssistChip(onClick = onRemove, label = { Text("Yours") })
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (plan.applied) {
                    "Added to your timetable · $courseCount courses, $sessionCount classes"
                } else if (courseCount == 0) {
                    "No courses in this plan yet. Add them below."
                } else {
                    "$courseCount courses, $sessionCount classes"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan.applied) {
                    Button(onClick = onRemove) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Remove from timetable")
                    }
                } else {
                    Button(onClick = onApply, enabled = courseCount > 0) {
                        Text("Use this plan")
                    }
                }
            }
        }
    }
}

/** Lets a student add a programme that is not in the catalogue. */
@Composable
fun AddMajorDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, year: Int, semester: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var year by remember { mutableIntStateOf(3) }
    var semester by remember { mutableStateOf("A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add your programme") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Programme name") },
                    placeholder = { Text("e.g. Computer Science") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                YearSemesterRow(
                    year = year,
                    semester = semester,
                    onYearChange = { year = it },
                    onSemesterChange = { semester = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, year, semester) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Adds a course to a programme plan so it can then be applied to the timetable. */
@Composable
fun AddPlanCourseDialog(
    planLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, credits: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add course") },
        text = {
            Column {
                Text(
                    text = planLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it.filter(Char::isDigit).take(2) },
                        label = { Text("Credits") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(name, code, credits.toIntOrNull() ?: 3)
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}