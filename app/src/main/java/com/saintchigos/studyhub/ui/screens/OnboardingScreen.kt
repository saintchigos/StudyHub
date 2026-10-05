package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.AddMajorDialog
import com.saintchigos.studyhub.ui.components.HowItWorks
import com.saintchigos.studyhub.ui.components.NotificationSettings
import com.saintchigos.studyhub.ui.components.PoweredBy
import com.saintchigos.studyhub.ui.components.ProgrammePlanCard
import com.saintchigos.studyhub.ui.components.SectionHeader
import com.saintchigos.studyhub.ui.components.SupportButton

/**
 * First run page. A student picks a programme and their timetable is created for them,
 * so nobody has to type a whole week out by hand. They can skip and build it manually.
 */
@Composable
fun OnboardingScreen(viewModel: StudyHubViewModel) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val stats by viewModel.planStats.collectAsStateWithLifecycle()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()
    var addingMajor by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Text("Welcome to StudyHub", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Choose your programme and we will set up your courses and timetable for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (plans.isEmpty()) {
                    item {
                        Text(
                            text = "Loading programmes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(plans, key = { it.planId }) { plan ->
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

                item {
                    Column {
                        SectionHeader("Programme not listed?")
                        Text(
                            text = "Add your own programme, then pick your year and semester.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { addingMajor = true }) { Text("Add my programme") }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        SectionHeader("Before you start")
                        HowItWorks()
                    }
                }

                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        SectionHeader("Reminders")
                        NotificationSettings()
                    }
                }

                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        SectionHeader("Need help?")
                        SupportButton()
                        PoweredBy()
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = {
                                if (it) viewModel.acceptTerms()
                            }
                        )
                        Text(
                            text = "I have read and accept the terms and conditions",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (!termsAccepted) viewModel.acceptTerms()
                                }
                        )
                        TextButton(onClick = { showTerms = true }) { Text("Read") }
                    }
                }

                item {
                    TextButton(
                        onClick = { viewModel.completeSetup() },
                        enabled = termsAccepted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (termsAccepted) "Continue"
                            else "Accept the terms to continue"
                        )
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
                    viewModel.completeSetup()
                }
            )
        }

        if (showTerms) {
            // Full page, not a dialog: a dialog cannot scroll reliably, and these terms
            // are the thing a student is agreeing to.
            LegalScreen(
                kind = LegalKind.Terms,
                onBack = { showTerms = false },
                onAccept = { viewModel.acceptTerms() }
            )
        }
    }
}