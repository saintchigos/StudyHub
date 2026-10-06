package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.NotificationLog
import com.saintchigos.studyhub.util.TimeUtil

/**
 * The reminders waiting to be read.
 *
 * Opening this sheet is what marks them read. That is deliberate: a separate
 * "mark all as read" button is one more thing to hunt for, and the student has
 * already read them by looking at them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInbox(
    entries: List<NotificationLog>,
    onDismiss: () -> Unit,
    onOpened: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Mark read as soon as it is on screen, not when it is scrolled past.
    LaunchedEffect(entries.size) { onOpened() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reminders", style = MaterialTheme.typography.titleLarge)
                val unread = entries.count { it.isUnread }
                if (unread > 0) {
                    Text(
                        "$unread to read",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (entries.isEmpty()) {
                EmptyInbox()
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        NotificationRow(entry)
                        HorizontalDivider()
                    }
                }
            }

            Text(
                "Reminders are filed here even if you dismiss them by mistake.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun NotificationRow(entry: NotificationLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // The dot is the only unread marker that is not text, so it survives being
        // read at a glance rather than being parsed.
        Surface(
            shape = CircleShape,
            color = if (entry.isUnread) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(top = 6.dp, end = 10.dp)
                .size(8.dp)
        ) {}

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (entry.isUnread) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                entry.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                TimeUtil.agoLabel(entry.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyInbox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Nothing waiting",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Class reminders land here so you can read them later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}