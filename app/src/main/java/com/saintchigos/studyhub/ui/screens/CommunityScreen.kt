package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.Connection
import com.saintchigos.studyhub.ui.Classmate
import com.saintchigos.studyhub.ui.CommunityViewModel

/**
 * The programme community: who else is in your year and semester, the class chat, and
 * the controls to block or report anyone.
 *
 * Visibility is decided by the database, not by this screen: people from other
 * programmes cannot be listed here, and blocked accounts disappear everywhere at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val account by viewModel.account.collectAsState()
    val membership by viewModel.membership.collectAsState()
    val classmates by viewModel.classmates.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var draft by remember { mutableStateOf("") }
    var reporting by remember { mutableStateOf<Classmate?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.refreshCommunity() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    label = { Text("Message your class") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (account == null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Sign in to join your class community",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "You need a StudyHub account so other students can see who is " +
                                    "posting, and so you can block or report anyone.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onOpenAccount) { Text("Sign in or register") }
                        }
                    }
                } else if (membership == null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Add a programme first",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "The community is scoped to your programme, year and semester. " +
                                    "Apply a programme in Settings and this fills in automatically.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${membership?.programmeName} · Year ${membership?.year} " +
                                    "Semester ${membership?.semester}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${classmates.size} classmate${if (classmates.size == 1) "" else "s"} in your cohort",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(classmates, key = { it.account.id }) { person ->
                ClassmateCard(
                    person = person,
                    onToggleConnection = { viewModel.toggleConnection(person) },
                    onToggleBlock = { viewModel.setBlocked(person, !person.blocked) },
                    onReport = { reporting = person }
                )
            }

            if (messages.isNotEmpty()) {
                item {
                    Text(
                        "Class chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(messages, key = { it.id }) { message ->
                    val mine = account?.id == message.senderId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (mine) 40.dp else 0.dp, end = if (mine) 0.dp else 40.dp)
                    ) {
                        Text(
                            text = if (mine) "You" else message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = message.body, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    val target = reporting
    if (target != null) {
        ReportDialog(
            person = target,
            onDismiss = { reporting = null },
            onSubmit = { reason, details ->
                viewModel.report(target, reason, details) { reporting = null }
            }
        )
    }
}

@Composable
private fun ClassmateCard(
    person: Classmate,
    onToggleConnection: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = person.account.displayName.ifEmpty { person.account.username },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "@${person.account.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onToggleConnection,
                    label = {
                        Text(
                            when {
                                person.blocked -> "Blocked"
                                person.connection?.status == Connection.STATUS_ACCEPTED ->
                                    "Connected"

                                person.connection?.status == Connection.STATUS_PENDING &&
                                    !person.outgoing -> "Accept request"

                                person.connection?.status == Connection.STATUS_PENDING &&
                                    person.outgoing -> "Request sent"

                                else -> "Connect"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) }
                )
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onToggleBlock) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (person.blocked) "Unblock" else "Block")
                }
                TextButton(onClick = onReport) {
                    Icon(Icons.Filled.Flag, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Report")
                }
            }
        }
    }
}

@Composable
private fun ReportDialog(
    person: Classmate,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val reasons = listOf(
        "Harassment or bullying",
        "Spam or scam",
        "Sharing someone's private information",
        "Inappropriate content",
        "Something else"
    )
    var selected by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report @${person.account.username}") },
        text = {
            Column {
                Text(
                    "Tell us what happened. Blocking is separate and can be done straight away.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = reason }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(if (selected == reason) "●" else "○", modifier = Modifier.width(20.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text("Anything else we should know (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected, details) }) { Text("Send report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}