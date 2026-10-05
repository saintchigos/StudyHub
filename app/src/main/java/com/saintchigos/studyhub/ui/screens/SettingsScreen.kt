package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.data.PlanWithProgramme
import com.saintchigos.studyhub.ui.CommunityViewModel
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.AddMajorDialog
import com.saintchigos.studyhub.ui.components.AddPlanCourseDialog
import com.saintchigos.studyhub.ui.components.AppSettingsSection
import com.saintchigos.studyhub.ui.components.HowItWorks
import com.saintchigos.studyhub.ui.components.InitialAvatar
import com.saintchigos.studyhub.ui.components.NotificationSettings
import com.saintchigos.studyhub.ui.components.ProgrammePlanCard
import com.saintchigos.studyhub.ui.components.ScreenHeader
import com.saintchigos.studyhub.ui.components.SettingsCard
import com.saintchigos.studyhub.ui.components.SettingsDivider
import com.saintchigos.studyhub.ui.components.SettingsGroupLabel
import com.saintchigos.studyhub.ui.components.SettingsRow
import com.saintchigos.studyhub.ui.components.planLabel
import com.saintchigos.studyhub.ui.theme.SuccessGreen
import com.saintchigos.studyhub.ui.theme.WarningAmber

/**
 * Settings, rebuilt as grouped cards.
 *
 * Every action is a tappable row with an icon and one short line of explanation.
 * The previous version ran headings, paragraphs and text buttons together in a single
 * column, which read as a wall of text and hid what was actually clickable.
 * Destructive actions sit in their own red-tinted group at the bottom.
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

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
    val custom = plans.filter { !it.applied }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Settings",
                    subtitle = "${courses.size} courses · ${courses.sumOf { it.credits }} credits"
                )
            }

            // ---- account ---------------------------------------------------
            item { SettingsGroupLabel("Account") }
            item {
                if (account == null) {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Filled.Person,
                            title = "Create an account",
                            subtitle = "Join your programme's community",
                            onClick = onOpenAccount
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Filled.Groups,
                            title = "Community",
                            subtitle = "Classmates, chat and help",
                            onClick = onOpenCommunity
                        )
                    }
                } else {
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialAvatar(account?.displayName ?: account?.username.orEmpty())
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account?.displayName ?: account?.username.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = buildString {
                                        append("@${account?.username.orEmpty()}")
                                        membership?.let {
                                            append(" · ")
                                            append(prettySlug(it.programmeSlug))
                                            append(" Yr ${it.year} ")
                                            append(it.semester)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Filled.Groups,
                            title = "Community",
                            subtitle = "Classmates, chat and help",
                            onClick = onOpenCommunity
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Log out",
                            subtitle = "Your timetable stays on this phone",
                            tint = WarningAmber,
                            onClick = { confirmSignOut = true }
                        )
                    }
                }
            }

            // ---- programme -------------------------------------------------
            item { SettingsGroupLabel("My programme") }
            if (applied.isEmpty()) {
                item {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Filled.School,
                            title = "No programme applied",
                            subtitle = "Add one and your timetable fills in",
                            tint = WarningAmber,
                            onClick = { addingMajor = true }
                        )
                    }
                }
            } else {
                items(applied, key = { "applied-${it.planId}" }) { plan ->
                    ProgrammePlanCard(
                        plan = plan,
                        stats = stats[plan.planId] ?: (0 to 0),
                        onApply = { viewModel.applyPlan(plan.planId) },
                        onRemove = { viewModel.removePlan(plan.planId) }
                    )
                }
            }

            item { SettingsGroupLabel("Add or switch") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.Add,
                        title = "Add my programme",
                        subtitle = "Only fills in what is missing",
                        onClick = { addingMajor = true }
                    )
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
            items(custom, key = { "edit-${it.planId}" }) { plan ->
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.School,
                        title = planLabel(plan),
                        subtitle = "Your own programme",
                        onClick = { addingCourseFor = plan }
                    )
                }
            }
            if (applied.isNotEmpty()) {
                item { SettingsGroupLabel("Semester") }
                item {
                    SettingsCard {
                        applied.forEachIndexed { index, plan ->
                            if (index > 0) SettingsDivider()
                            SettingsRow(
                                icon = Icons.Filled.EventBusy,
                                title = "End ${planLabel(plan)}",
                                subtitle = "Clears classes and reminders, keeps work",
                                tint = WarningAmber,
                                onClick = { endingSemesterFor = plan }
                            )
                        }
                    }
                }
            }

            // ---- reminders --------------------------------------------------
            item { SettingsGroupLabel("Reminders") }
            item {
                SettingsCard { NotificationSettings() }
            }

            // ---- appearance -------------------------------------------------
            item { SettingsGroupLabel("Appearance") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = if (themeMode == "dark") Icons.Filled.DarkMode
                        else Icons.Filled.LightMode,
                        title = "Appearance",
                        subtitle = when (themeMode) {
                            "dark" -> "Dark"
                            "light" -> "Light"
                            else -> "Follows your phone"
                        },
                        onClick = {
                            viewModel.setThemeMode(
                                when (themeMode) {
                                    "dark" -> "light"
                                    "light" -> "system"
                                    else -> "dark"
                                }
                            )
                        }
                    )
                }
            }
            item { SettingsCard { AppSettingsSection(viewModel = viewModel) } }

            // ---- legal ------------------------------------------------------
            item { SettingsGroupLabel("Legal") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.Description,
                        title = "Terms and conditions",
                        subtitle = if (termsAccepted) "Accepted on this device"
                        else "Not accepted yet",
                        tint = if (termsAccepted) SuccessGreen else WarningAmber,
                        onClick = { onOpenLegal(LegalKind.Terms) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = "Privacy",
                        subtitle = "What we store and what others can see",
                        onClick = { onOpenLegal(LegalKind.Privacy) }
                    )
                }
            }

            // ---- help -------------------------------------------------------
            item { SettingsGroupLabel("Help") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.SupportAgent,
                        title = "Contact support",
                        subtitle = "WhatsApp +266 6284 8760",
                        onClick = { openSupportChat(context) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.RestartAlt,
                        title = "Return to start-up page",
                        subtitle = "Your data stays as it is",
                        onClick = { viewModel.restartSetup() }
                    )
                }
            }
            item {
                val showTips by viewModel.showStudyTips.collectAsStateWithLifecycle()
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.Lightbulb,
                        title = "Study tips on Home",
                        subtitle = if (showTips) "Showing" else "Hidden",
                        tint = SuccessGreen,
                        onClick = { viewModel.setShowStudyTips(!showTips) }
                    )
                }
            }
            item { HowItWorks() }

            // ---- danger -----------------------------------------------------
            item { SettingsGroupLabel("Danger zone") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Filled.DeleteForever,
                        title = "Delete all my data",
                        subtitle = "Every course, class, assignment and exam",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { confirmDeleteAll = true }
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = "StudyHub $versionName",
                        subtitle = "A student dashboard by Chigos Media"
                    )
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
            AlertDialog(
                onDismissRequest = { endingSemesterFor = null },
                title = { Text("End ${planLabel(ending)}?") },
                text = {
                    Text(
                        "This clears the class times and reminders for " +
                            "${planLabel(ending)}. Your courses, assignments and exams are kept."
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

/** Opens the Chigos Media WhatsApp chat. */
private fun openSupportChat(context: android.content.Context) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://wa.me/26662848760")
    )
    runCatching { context.startActivity(intent) }
}

/** Turns a stored slug like "computers-and-statistics" back into readable words. */
private fun prettySlug(slug: String): String = slug
    .split('-', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
