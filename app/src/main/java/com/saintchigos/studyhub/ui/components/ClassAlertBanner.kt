package com.saintchigos.studyhub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.reminder.ClassReminder
import com.saintchigos.studyhub.reminder.ReminderRules
import com.saintchigos.studyhub.ui.theme.WarningAmber

/** Loud in-app banner so the reminder still lands when the app is open. */
@Composable
fun ClassAlertBanner(
    reminder: ClassReminder?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = reminder != null,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        val r = reminder ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = WarningAmber,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (r.leadMinutes > 0)
                            "${r.session.courseCode} starts in ${r.leadMinutes} min"
                        else
                            "${r.session.courseCode} has started",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(r.session.courseName)
                            if (r.session.room.isNotBlank()) {
                                append("  ·  ")
                                append(r.session.room)
                            }
                            append("  ·  ")
                            append(r.whenLabel)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}