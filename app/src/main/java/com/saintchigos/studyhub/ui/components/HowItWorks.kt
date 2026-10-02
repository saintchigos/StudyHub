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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Step(val title: String, val detail: String)

private val STEPS = listOf(
    Step(
        "1. Pick your programme",
        "Choose your major, year and semester. StudyHub fills in your courses and " +
            "timetable for you, so you never have to type a whole week out."
    ),
    Step(
        "2. Get reminded before class",
        "We alert you 10 minutes before the first class of your day, and 5 minutes " +
            "before every other class. You get a full screen alert with sound and vibration."
    ),
    Step(
        "3. Keep track of your work",
        "Add assignments and exams to each course. Upcoming deadlines and exams show " +
            "on your home screen so nothing slips."
    ),
    Step(
        "4. Edit anything, any time",
        "Tap a class in your timetable to change its day, time or room. Add or remove " +
            "classes and courses whenever your schedule changes."
    ),
    Step(
        "5. Semesters and support",
        "When your semester ends, tell us in Setup and load your next one. Need help? " +
            "Message Chigos Media on WhatsApp any time."
    )
)

/** Explains how StudyHub works, for new students and for staff. */
@Composable
fun HowItWorks(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("How StudyHub works", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        STEPS.forEach { step ->
            StepRow(step)
        }
    }
}

@Composable
private fun StepRow(step: Step) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = step.title.substringBefore(".").ifBlank { "1" },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title.substringAfter("."),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}