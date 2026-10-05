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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val detail: String
)

private val FEATURES = listOf(
    Feature(
        Icons.Filled.School,
        "Timetable from your programme",
        "Choose your major, year and semester and StudyHub fills in your courses and " +
            "class times. Tap any class to move it, rename it or change the room."
    ),
    Feature(
        Icons.Filled.NotificationsActive,
        "Alerts before every class",
        "10 minutes before your first class of the day, 5 minutes before the rest. " +
            "Loud enough to hear in a bag, and the lead time is yours to change."
    ),
    Feature(
        Icons.Filled.Timer,
        "Focus timer that keeps going",
        "Pick 15, 25, 45 or 60 minutes, start it and put the phone down. You get a " +
            "notification when the block ends and the time is added to your study log."
    ),
    Feature(
        Icons.Filled.Alarm,
        "Wake-up alarms that ring in silent mode",
        "For a lab session, an early class or a library slot. Each alarm picks the " +
            "days it repeats and re-arms itself, even after a restart."
    ),
    Feature(
        Icons.Filled.CheckCircle,
        "Tasks and exams in one list",
        "Assignments and exams sit on your Home screen with what is due next and " +
            "what is overdue. Ticking one off also counts as study time."
    ),
    Feature(
        Icons.Filled.EditCalendar,
        "Works offline, always",
        "Everything is stored on this phone. No signal means no lost work, no blank " +
            "screen and no waiting."
    )
)

/** Explains what the app does, for new students and for anyone reviewing it. */
@Composable
fun HowItWorks(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "How StudyHub works",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Six things, and nothing else in the way",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        FEATURES.forEachIndexed { index, feature ->
            FeatureRow(feature)
            if (index < FEATURES.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    feature.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
