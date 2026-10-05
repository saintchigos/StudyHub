package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SECTIONS = listOf(
    "What stays on your phone" to listOf(
        "Your courses, class times, assignments, exams and any programme you add " +
            "yourself are stored in a database on this device only.",
        "If you never create an account, nothing about you is ever sent anywhere."
    ),
    "What an account stores" to listOf(
        "Creating an account keeps your username, display name and which programme, " +
            "year and semester you are in.",
        "Your password is never stored as plain text. StudyHub keeps only a salted " +
            "hash of it, so it cannot be read back out of the app.",
        "Your timetable, assignments and exams are not uploaded with your account."
    ),
    "What other students can see" to listOf(
        "In the community, other students in the same programme and semester can see " +
            "your display name and your messages.",
        "You can block anyone. A blocked student disappears from your class list and " +
            "can no longer message you.",
        "You can report a student to us through Settings if they misbehave."
    ),
    "Permissions" to listOf(
        "Notifications and exact alarms are used only to warn you before class. You " +
            "can turn reminders off without affecting your timetable.",
        "StudyHub does not ask for your contacts, photos, location or microphone."
    ),
    "Deleting your data" to listOf(
        "Delete all my data in Settings removes every course, class, assignment and " +
            "exam from this device immediately.",
        "Deleting your account removes your community profile, messages and " +
            "connections."
    )
)

/**
 * Privacy policy, kept separate from the terms so a student can read exactly what is
 * collected about them without wading through legal wording.
 */
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text("Privacy")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 380.dp)
            ) {
                SECTIONS.forEach { (heading, paragraphs) ->
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Questions? Ask us on WhatsApp: +266 6284 8760",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PoweredBy()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
