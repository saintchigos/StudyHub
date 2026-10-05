package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.AddMajorDialog
import com.saintchigos.studyhub.ui.components.NotificationSettings
import com.saintchigos.studyhub.ui.components.PoweredBy
import com.saintchigos.studyhub.ui.components.ProgrammePlanCard

/**
 * First run, told as three short pages instead of one long form.
 *
 * The old version put the programme list, notification sliders, support link,
 * five-paragraph explanation and the terms checkbox on a single scrolling screen,
 * so the first thing a new student saw was a wall of text before they could do
 * anything. Each step now explains itself, and only one asks for a decision.
 *
 * Step 1 explains the idea and starts the programme picker.
 * Step 2 handles reminders, alarms and focus.
 * Step 3 handles accounts, community and the terms.
 */
@Composable
fun OnboardingScreen(viewModel: StudyHubViewModel) {
    val setupComplete by viewModel.setupComplete.collectAsStateWithLifecycle()

    // Completing step one should move forward, not drop the student into an empty
    // app, so the pager advances and the final step owns the continue button.
    var step by remember { mutableStateOf(0) }

    // MainActivity only shows onboarding while setup is incomplete, but guard anyway
    // so a tap that flips the flag mid-compose cannot leave a dead screen up.
    if (setupComplete) return

    Column(modifier = Modifier.fillMaxSize()) {
        StepDots(current = step, total = 3)

        LazyColumn(
            // weight, not fillMaxSize: the scroll area must leave room for the Back
            // and Skip row underneath, or those buttons are pushed off screen.
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                0 -> item { ProgrammeStep(viewModel) { step = 1 } }
                1 -> item { ToolsStep() }
                else -> item { AccountStep(viewModel) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.height(52.dp)
                ) { Text("Back") }
            }
            if (step < 2) {
                TextButton(
                    onClick = { step = 2 },
                    modifier = Modifier.height(52.dp)
                ) { Text("Skip") }
            } else {
                TextButton(
                    onClick = { step = 0 },
                    modifier = Modifier.height(52.dp)
                ) { Text("Start over") }
            }
        }
    }
}

@Composable
private fun ProgrammeStep(viewModel: StudyHubViewModel, onNext: () -> Unit) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val stats by viewModel.planStats.collectAsStateWithLifecycle()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()
    var addingMajor by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    StepHeading(
        title = "Welcome to StudyHub",
        subtitle = "Your whole semester in one app: timetable, tasks, exams, focus timer and wake-up alarms."
    )

    Spacer(Modifier.height(4.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Pick your programme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose your major, year and semester. StudyHub fills in your courses and " +
                    "timetable for you, so you never type a whole week out by hand.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (plans.isEmpty()) {
        Text(
            "Loading programmes…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    plans.forEach { plan ->
        Spacer(Modifier.height(10.dp))
        ProgrammePlanCard(
            plan = plan,
            stats = stats[plan.planId] ?: (0 to 0),
            onApply = {
                viewModel.applyPlan(plan.planId)
                viewModel.completeSetup()
            },
            onRemove = { viewModel.removePlan(plan.planId) }
        )
    }

    Spacer(Modifier.height(10.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Programme not listed?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Add your own, then pick your year and semester.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { addingMajor = true }) { Text("Add my programme") }
        }
    }

    Spacer(Modifier.height(10.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("By continuing you agree to our", style = MaterialTheme.typography.bodyMedium)
            Row {
                TextButton(onClick = { showTerms = true }) { Text("terms and conditions") }
                TextButton(onClick = onNext) { Text("Next") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { if (it) viewModel.acceptTerms() }
                )
                Text(
                    "I have read and accept the terms",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
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
                viewModel.completeSetup()
            }
        )
    }

    if (showTerms) {
        LegalScreen(
            kind = LegalKind.Terms,
            onBack = { showTerms = false },
            onAccept = { viewModel.acceptTerms() }
        )
    }
}

/** Step two explains the reminders and timers, and lets them be tuned now. */
@Composable
private fun ToolsStep() {
    val context = androidx.compose.ui.platform.LocalContext.current

    StepHeading(
        title = "Alerts that actually wake you up",
        subtitle = "Three ways this app gets your attention, each switched off by default so it never nags you."
    )

    Spacer(Modifier.height(4.dp))

    FeatureRow(
        icon = Icons.Filled.NotificationsActive,
        title = "Before every class",
        detail = "A loud heads-up 10 minutes before your first class of the day, and 5 " +
            "minutes before the rest. Works with the phone in your bag."
    )

    Spacer(Modifier.height(10.dp))
    FeatureRow(
        icon = Icons.Filled.Alarm,
        title = "Wake-up alarms",
        detail = "Set your own alarms for a lab, an early class or a library slot. They ring " +
            "even in silent mode, and re-arm themselves every day."
    )

    Spacer(Modifier.height(10.dp))
    FeatureRow(
        icon = Icons.Filled.Timer,
        title = "Focus timer",
        detail = "Pick 15, 25, 45 or 60 minutes. When the block ends you are told, and the " +
            "time is logged so you can see where your study hours went."
    )

    Spacer(Modifier.height(14.dp))
    Text(
        "Tune the reminders now, change them any time in More settings.",
        style = MaterialTheme.typography.labelLarge
    )
    NotificationSettings()
}

@Composable
private fun AccountStep(viewModel: StudyHubViewModel) {
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()
    var showTerms by remember { mutableStateOf(false) }

    StepHeading(
        title = "One more thing",
        subtitle = "You can skip all of this. Nothing here is needed to use the app."
    )

    Spacer(Modifier.height(4.dp))

    FeatureRow(
        icon = Icons.Filled.CheckCircle,
        title = "No account needed",
        detail = "Your timetable and work stay on this phone and work offline. Create an " +
            "account later if you want to join the campus community."
    )

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = termsAccepted,
            onCheckedChange = { if (it) viewModel.acceptTerms() }
        )
        Text(
            "I have read and accept the terms and conditions",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Spacer(Modifier.height(6.dp))
    TextButton(onClick = { showTerms = true }) { Text("Read the terms") }

    Spacer(Modifier.height(10.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ready to go", style = MaterialTheme.typography.titleMedium)
            Text(
                "You can pick a programme later in Courses, so tap continue whenever you like.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.completeSetup() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Continue to the app") }
        }
    }

    PoweredBy()

    if (showTerms) {
        LegalScreen(kind = LegalKind.Terms, onBack = { showTerms = false })
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            Text(
                text = "●",
                fontSize = if (index == current) {
                    androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                } else {
                    androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
                },
                color = if (index == current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

