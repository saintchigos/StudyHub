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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import com.saintchigos.studyhub.ui.components.ScreenHeader
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.PlanWithProgramme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saintchigos.studyhub.ui.CommunityViewModel
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
import com.saintchigos.studyhub.ui.components.planLabel

/**
 * Settings. Everything the student can configure lives here: programme and semester,
 * reminder permissions and timing, appearance, terms and conditions, support and
 * their data. Nothing here deletes anything without an explicit confirmation.
 */
@Composable
fun SettingsScreen(
    viewModel: StudyHubViewModel,
    communityViewModel: CommunityViewModel,
    onOpenCommunity: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenLegal: (LegalKind) -> Unit = {}
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val stats by viewModel.planStats.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()

    var addingMajor by remember { mutableStateOf(false) }
    var addingCourseFor by remember { mutableStateOf<PlanWithProgramme?>(null) }
    var endingSemesterFor by remember { mutableStateOf<PlanWithProgramme?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val account by communityViewModel.account.collectAsStateWithLifecycle()
    val membership by communityViewModel.membership.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var versionName by remember { mutableStateOf("1.0") }
    LaunchedEffect(Unit) {
        versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    val applied = plans.filter { it.applied }
    val custom = plans.filter { it.isCustom }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Settings",
                    subtitle = "Programme, reminders, appearance and your data"
                )
            }

            // ---- programme -------------------------------------------------
            item { SectionHeader("My programme") }
            item {
                if (applied.isEmpty()) {
                    Text(
                        text = "No programme applied yet. Choose one below and your courses " +
                            "and timetable are filled in for you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(applied, key = { "applied-${it.planId}" }) { plan ->
                ProgrammePlanCard(
                    plan = plan,
                    stats = stats[plan.planId] ?: (0 to 0),
                    onApply = { viewModel.applyPlan(plan.planId) },
                    onRemove = { viewModel.removePlan(plan.planId) }
                )
            }

            item {
                Column {
                    SectionHeader("Switch or add a programme")
                    Text(
                        text = "Switching never deletes your work. Adding a programme only " +
                            "fills in what is missing, and you can remove it again at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { addingMajor = true }) { Text("Add my programme") }
                }
            }
            items(plans.filter { !it.applied }, key = { "pick-${it.planId}" }) { plan ->
                ProgrammePlanCard(
                    plan = plan,
                    stats = stats[plan.planId] ?: (0 to 0),
                    onApply = { viewModel.applyPlan(plan.planId) },
                    onRemove = { viewModel.removePlan(plan.planId) }
                )
            }
            if (custom.isNotEmpty()) {
                items(custom, key = { "edit-${it.planId}" }) { plan ->
                    Column {
                        Text(planLabel(plan), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { addingCourseFor = plan }) {
                            Text("Add a course to this programme")
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeader("Start-up page")
                    Text(
                        text = "Go back to the first-run programme picker. Your courses, " +
                            "timetable, assignments and exams stay as they are.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.restartSetup() }) {
                        Text("Return to start-up page")
                    }
                }
            }

            // ---- semester --------------------------------------------------
            item { SectionHeader("Semester") }
            item {
                Column {
                    Text(
                        text = "When your semester ends, clear the old class times and " +
                            "reminders so you stop being warned about classes you no longer " +
                            "attend. Your courses, assignments and exams are kept.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    if (applied.isEmpty()) {
                        Text(
                            text = "No programme is applied, so there is nothing to end yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        applied.forEach { plan ->
                            TextButton(onClick = { endingSemesterFor = plan }) {
                                Text("End ${planLabel(plan)}")
                            }
                        }
                    }
                }
            }

            // ---- reminders and appearance -----------------------------------
            item {
                Column {
                    SectionHeader("Reminders")
                    NotificationSettings()
                }
            }
            item {
                Column {
                    SectionHeader("Appearance and alert style")
                    AppSettingsSection(viewModel = viewModel)
                }
            }

            // ---- account and community ---------------------------------------
            item {
                Column {
                    SectionHeader("Your account and community")
                    if (account == null) {
                        Text(
                            text = "Create a password to join the community for your " +
                                "programme and year. Find classmates, chat about the " +
                                "semester and ask for help. Your timetable works without " +
                                "an account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onOpenAccount) { Text("Create account") }
                            TextButton(onClick = onOpenCommunity) { Text("Open community") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (account?.displayName
                                            ?: account?.username
                                            ?: "?").take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account?.displayName ?: account?.username.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = buildString {
                                        append("@${account?.username.orEmpty()}")
                                        membership?.let {
                                            append("  ·  ")
                                            append(prettySlug(it.programmeSlug))
                                            append(" Year ${it.year}, Semester ${it.semester}")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onOpenCommunity) { Text("Open community") }
                            TextButton(onClick = { confirmSignOut = true }) { Text("Log out") }
                        }
                    }
                }
            }

            // ---- terms and conditions ---------------------------------------
            item {
                Column {
                    SectionHeader("Terms and conditions")
                    Text(
                        text = "Your timetable is stored on your device. If you create an " +
                            "account, only your username, display name and programme are kept " +
                            "for the community.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    TermsSummary()
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onOpenLegal(LegalKind.Terms) }) {
                            Text("Read full terms and conditions")
                        }
                        TextButton(onClick = { onOpenLegal(LegalKind.Privacy) }) {
                            Text("Privacy")
                        }
                    }
                    Text(
                        text = if (termsAccepted) "You accepted the terms on this device."
                        else "You have not accepted the terms yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- support and about ------------------------------------------
            item {
                Column {
                    SectionHeader("Help and support")
                    HowItWorks()
                    Spacer(Modifier.height(8.dp))
                    SupportButton()
                }
            }
            item {
                Column {
                    SectionHeader("About")
                    Text("StudyHub", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "A student dashboard by Chigos Media.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PoweredBy()
                }
            }

            item {
                Column {
                    SectionHeader("Your data")
                    Text(
                        text = "${courses.size} courses and ${courses.sumOf { it.credits }} " +
                            "credits in your timetable. You can remove any course, class, " +
                            "assignment or exam yourself at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { confirmDeleteAll = true }) {
                        Text("Delete all my data")
                    }
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
                            planLabel(ending) + ". Your courses, assignments and exams are kept."
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

        if (confirmSignOut) {
            AlertDialog(
                onDismissRequest = { confirmSignOut = false },
                title = { Text("Log out?") },
                text = {
                    Text(
                        "You can log back in with the same username and password. " +
                            "Your timetable stays on this device either way."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        communityViewModel.signOut()
                        confirmSignOut = false
                    }) { Text("Log out") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
                }
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

/** Turns a stored slug like "computers-and-statistics" back into readable words. */
private fun prettySlug(slug: String): String = slug
    .split('-', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** Plain-language version of the terms, shown without needing to open the dialog. */
@Composable
private fun TermsSummary() {
    val points = listOf(
        "Your timetable, courses, assignments and exams belong to you.",
        "Everything stays on this device. Nothing is uploaded.",
        "An account is optional. You only need one for the community.",
        "Your password is stored as a salted hash, never as plain text.",
        "You can block or report anyone in the community.",
        "Reminders need notification and alarm permission to arrive on time.",
        "Timetable templates are a starting point. Confirm your real schedule " +
            "with your department.",
        "The app is provided as is, without warranty."
    )
    points.forEach { point ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp).padding(top = 2.dp)
            )
            Text(
                text = point,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

