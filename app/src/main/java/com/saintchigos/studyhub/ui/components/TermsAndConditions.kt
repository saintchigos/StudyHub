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
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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

private val SECTIONS = listOf(
    "About StudyHub" to listOf(
        "StudyHub is a student dashboard built by Chigos Media. It keeps your weekly " +
            "timetable, courses, assignments and exams in one place, and warns you before " +
            "class so you are never late because you forgot.",
        "All of your data is stored only on this device. StudyHub has no account server " +
            "and does not upload your timetable, courses or personal information to anyone."
    ),
    "Your data" to listOf(
        "Courses, class times, assignments and exams belong to you. You can edit or " +
            "delete any of them at any time, including deleting everything from Setup.",
        "Programmes you add yourself stay on the device so you can reuse them later."
    ),
    "Reminders and notifications" to listOf(
        "Class reminders use your device's alarm system so they can arrive on time even " +
            "when StudyHub is closed. That needs permission for notifications and, on some " +
            "Android versions, permission to schedule exact alarms.",
        "Turning reminders off in Setup stops the alerts but leaves your timetable alone."
    ),
    "Timetable accuracy" to listOf(
        "Timetable templates in the app are a convenience supplied as a starting point. " +
            "Always confirm your real schedule with your department, because rooms and " +
            "times change and Chigos Media is not responsible for missed classes caused by " +
            "out-of-date information."
    ),
    "Acceptable use" to listOf(
        "Use StudyHub for your own study organisation. Do not attempt to disrupt the app " +
            "for other users or misuse the support contact.",
        "The app is provided as is, without warranty. We may update or change features as " +
            "the app improves."
    )
)

/**
 * Terms and conditions. A student has to accept these once before they can finish
 * setting up the app.
 */
@Composable
fun TermsAndConditionsDialog(
    onAccept: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var accepted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text("Terms and conditions")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 380.dp)) {
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    Spacer(Modifier.size(6.dp))
                    Text("I have read and accept the terms", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                PoweredBy()
            }
        },
        confirmButton = {
            TextButton(
                enabled = accepted,
                onClick = {
                    onAccept()
                    onDismiss?.invoke()
                }
            ) { Text("Accept") }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
