package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.PlanWithProgramme
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.AddMajorDialog
import com.saintchigos.studyhub.ui.components.AddPlanCourseDialog
import com.saintchigos.studyhub.ui.components.AppSettingsSection
import com.saintchigos.studyhub.ui.components.HowItWorks
import com.saintchigos.studyhub.ui.components.NotificationSettings
import com.saintchigos.studyhub.ui.components.PoweredBy
import com.saintchigos.studyhub.ui.components.ProgrammePlanCard
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.SupportButton
import com.saintchigos.studyhub.ui.components.TermsAndConditionsDialog
import com.saintchigos.studyhub.ui.components.planLabel

/**
 * Programme settings. This is where a student switches major, adds a programme that
 * is not in the catalogue, or removes a plan they no longer want. Nothing here
 * deletes their assignments or exams.
 */
@Composable
fun SetupScreen(
    viewModel: StudyHubViewModel,
    onClose: (() -> Unit)? = null
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val stats by viewModel.planStats.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var addingMajor by remember { mutableStateOf(false) }
    var addingCourseFor by remember { mutableStateOf<PlanWithProgramme?>(null) }
    var endingSemesterFor by remember { mutableStateOf<PlanWithProgramme?>(null) }
    var showTerms by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var versionName by remember { mutableStateOf("1.0") }
    LaunchedEffect(Unit) {
        versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Setup",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (onClose != null) {
                            TextButton(onClick = onClose) { Text("Done") }
                        }
                    }
                    Text(
                        text = "Your programme and timetable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionHeader("Applied programmes")
                if (plans.none { it.applied }) {
                    Text(
                        text = "No programme applied yet. Pick one below to fill in your timetable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(plans.filter { it.applied }, key = { it.planId }) { plan ->
                ProgrammePlanCard(
                    plan = plan,
                    stats = stats[plan.planId] ?: (0 to 0),
                    onApply = { viewModel.applyPlan(plan.planId) },
                    onRemove = { viewModel.removePlan(plan.planId) }
                )
            }

            item {
                SectionHeader("Add another programme")
                Text(
                    text = "Switching never deletes your work. Adding a plan only fills in what is missing, and you can remove it again at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { addingMajor = true }) { Text("Add my programme") }
            }

            items(plans, key = { "pick-${it.planId}" }) { plan ->
                ProgrammePlanCard(
                    plan = plan,
                    stats = stats[plan.planId] ?: (0 to 0),
                    onApply = { viewModel.applyPlan(plan.planId) },
                    onRemove = { viewModel.removePlan(plan.planId) }
                )
            }

            val custom = plans.filter { it.isCustom }
            if (custom.isNotEmpty()) {
                item { SectionHeader("Courses in your programmes") }
                items(custom, key = { "edit-${it.planId}" }) { plan ->
                    Column {
                        Text(
                            text = planLabel(plan),
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(onClick = { addingCourseFor = plan }) {
                            Text("Add a course to this programme")
                        }
                    }
                }
            }

            item {
                SectionHeader("Class reminders")
                NotificationSettings(modifier = Modifier.padding(top = 4.dp))
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("Semester ended?")
                    Text(
                        text = "Ending the semester clears the class times and reminders for the applied plan so you stop getting alerts for old classes. Your courses, assignments and exams stay safe, and you can load your next semester's timetable straight away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val applied = plans.filter { it.applied }
                    if (applied.isEmpty()) {
                        Text(
                            text = "No plan is applied, so there is nothing to end yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        applied.forEach { plan ->
                            TextButton(onClick = { endingSemesterFor = plan }) {
                                Text("End ${plan.programmeName} Semester ${plan.semester}")
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("App settings")
                    AppSettingsSection(
                        viewModel = viewModel,
                        versionName = versionName,
                        onShowTerms = { showTerms = true }
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("How it works")
                    HowItWorks()
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("Support")
                    SupportButton()
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("Danger zone")
                    Text(
                        text = "Remove every course, class, assignment and exam from this device. Your programme catalogue is kept so you can set up again straight away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { confirmDeleteAll = true }) {
                        Text("Delete all my data")
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SectionHeader("Start-up page")
                    Text(
                        text = "Go back to the first-run programme picker. Your courses, timetable, assignments and exams stay as they are.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { viewModel.restartSetup() }) {
                        Text("Return to start-up page")
                    }
                    Text(
                        text = "${courses.size} courses in your timetable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PoweredBy()
                }
            }
        }

        if (addingMajor) {
            AddMajorDialog(
                onDismiss = { addingMajor = false },
                onConfirm = { name, year, semester ->
                    viewModel.addProgramme(name, year, semester)
                    addingMajor = false
                }
            )
        }

        val target = addingCourseFor
        if (target != null) {
            AddPlanCourseDialog(
                planLabel = planLabel(target),
                onDismiss = { addingCourseFor = null },
                onConfirm = { name, code, credits ->
                    viewModel.addPlanCourse(target.planId, name, code, credits)
                    addingCourseFor = null
                }
            )
        }

        val ending = endingSemesterFor
        if (ending != null) {
            val endingTitle = "End " + planLabel(ending) + "?"
            AlertDialog(
                onDismissRequest = { endingSemesterFor = null },
                title = { Text(endingTitle) },
                text = {
                    Text(
                        "This clears the class times and reminders for " +
                            "${ending.programmeName} Year ${ending.year} Semester ${ending.semester}. " +
                            "Your courses, assignments and exams are kept."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.endSemester(ending.planId)
                        endingSemesterFor = null
                    }) { Text("End semester") }
                },
                dismissButton = {
                    TextButton(onClick = { endingSemesterFor = null }) { Text("Cancel") }
                }
            )
        }

        if (showTerms) {
            TermsAndConditionsDialog(
                onAccept = { viewModel.acceptTerms() },
                onDismiss = { showTerms = false }
            )
        }

        if (confirmDeleteAll) {
            AlertDialog(
                onDismissRequest = { confirmDeleteAll = false },
                title = { Text("Delete all your data?") },
                text = {
                    Text(
                        "Every course, class time, assignment and exam on this device will " +
                            "be removed. This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllData()
                        confirmDeleteAll = false
                    }) { Text("Delete everything") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
                }
            )
        }
    }
}
