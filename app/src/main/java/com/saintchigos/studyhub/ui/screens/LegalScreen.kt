package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.ui.components.PRIVACY_SECTIONS
import com.saintchigos.studyhub.ui.components.PoweredBy
import com.saintchigos.studyhub.ui.components.TERMS_SECTIONS

/**
 * Terms and privacy as full pages rather than dialogs.
 *
 * A dialog cannot scroll reliably: Material 3 already makes dialog text scrollable,
 * so nesting another verticalScroll inside it either swallows the gesture or leaves
 * the content unreachable on a small screen. A page scrolls the same way as the rest
 * of the app, keeps the back gesture working, and gives room to actually read the text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    kind: LegalKind,
    onBack: () -> Unit,
    onAccept: (() -> Unit)? = null
) {
    val sections = if (kind == LegalKind.Terms) TERMS_SECTIONS else PRIVACY_SECTIONS
    val icon: ImageVector = if (kind == LegalKind.Terms) {
        Icons.Filled.Gavel
    } else {
        Icons.Filled.PrivacyTip
    }
    var accepted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kind.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp).size(22.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = kind.blurb,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            sections.forEach { (heading, paragraphs) ->
                item(key = heading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            if (onAccept != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "I have read and accept the terms",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                enabled = accepted,
                                onClick = {
                                    onAccept()
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Accept and continue") }
                        }
                    }
                }
            }

            item { PoweredBy() }
        }
    }
}

enum class LegalKind(val title: String, val blurb: String) {
    Terms(
        title = "Terms and conditions",
        blurb = "The rules for using StudyHub. Read these before you rely on the app " +
            "for your timetable or join the community."
    ),
    Privacy(
        title = "Privacy",
        blurb = "Exactly what StudyHub stores, what other students can see, and how " +
            "to get rid of it."
    )
}
