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

val TERMS_SECTIONS = listOf(
    "About StudyHub" to listOf(
        "StudyHub is a student dashboard built by Chigos Media. It keeps your weekly " +
            "timetable, courses, assignments and exams in one place, and warns you before " +
            "class so you are never late because you forgot.",
        "You can use every part of StudyHub without an account. Creating one is only " +
            "needed if you want the community features: finding classmates, chatting " +
            "about your semester and asking for help.",
        "Your timetable, courses, assignments and exams stay on your device. StudyHub " +
            "does not upload them anywhere."
    ),
    "Your account" to listOf(
        "An account needs a username, a display name other students see, and a " +
            "password you choose.",
        "Your password is never stored as plain text. StudyHub keeps only a salted hash " +
            "of it, so it cannot be read back out of the app.",
        "An account lets other students in your programme and year see your display " +
            "name and message you in the community. You can block anyone at any time."
    ),
    "Your data" to listOf(
        "Courses, class times, assignments and exams belong to you. You can edit or " +
            "delete any of them at any time, including deleting everything from Setup.",
        "Programmes you add yourself stay on the device so you can reuse them later.",
        "Delete all my data in Settings removes your timetable immediately. Deleting " +
            "your account removes your community profile, messages and connections."
    ),
    "Community conduct" to listOf(
        "Post what is useful to other students. Do not post anyone's personal " +
            "information, and do not use the community to harass or cheat.",
        "You can report a student from the community screen and we will review it. " +
            "Serious abuse gets an account removed."
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
    "Community features" to listOf(
        "The community lets students in the same programme and semester find each " +
            "other, send messages and ask for help with coursework.",
        "You choose whether to join. If you do not create an account, nothing you do " +
            "in StudyHub is visible to anyone else.",
        "Until the community server is switched on, accounts and messages stay on " +
            "this device only, so a classmate on another phone will not see you yet."
    ),
    "Acceptable use" to listOf(
        "Use StudyHub for your own study organisation and for treating other students " +
            "decently in the community. Do not attempt to disrupt the app for other " +
            "users or misuse the support contact.",
        "The app is provided as is, without warranty. We may update or change features " +
            "as the app improves.",
        "Chigos Media can remove an account that breaks these terms."
    )
)

/**
 * Kept only so onboarding can accept the terms without leaving the first-run flow.
 * Settings and anywhere else uses `LegalScreen`, which scrolls properly.
 */
@Deprecated(
    "Use LegalScreen: a dialog cannot scroll reliably on a small screen.",
    ReplaceWith("com.saintchigos.studyhub.ui.screens.LegalScreen")
)
@Composable
fun TermsAndConditionsDialog(
    onAccept: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var accepted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("Terms and conditions") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TERMS_SECTIONS.forEach { (heading, paragraphs) ->
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "I have read and accept the terms",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
