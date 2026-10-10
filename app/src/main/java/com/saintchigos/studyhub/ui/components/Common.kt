package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.ui.theme.CoursePalette

fun courseColor(colorIndex: Int): Color =
    CoursePalette[((colorIndex % CoursePalette.size) + CoursePalette.size) % CoursePalette.size]

@Composable
fun CourseAvatar(
    colorIndex: Int,
    code: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val color = courseColor(colorIndex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, color.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // White on the solid colour: every CoursePalette entry clears 4.5:1 against it.
        Text(
            text = code.take(3).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun CourseTag(code: String, colorIndex: Int, modifier: Modifier = Modifier) {
    val color = courseColor(colorIndex)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = code,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    // A wash of the stat's own colour over a clean card, so a row of stats reads as
    // a row of different things rather than three identical grey boxes.
    val tinted = accent.copy(alpha = 0.14f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = tinted),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerticalDivider(height: androidx.compose.ui.unit.Dp = 28.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(height)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Consistent title block for every screen, so headings line up and share the same
 * weight and spacing across tabs.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * The bell, with a badge counting reminders the student has not opened yet.
 *
 * Shown only when something is actually unread. A permanent bell with a permanent
 * zero trains people to stop looking at it, and on a phone with a small screen it
 * is also one less thing competing with the date.
 */
@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (unreadCount <= 0) return

    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications, $unreadCount to read"
            )
        }
        // Anchored to the top end of the button rather than centred, so the number
        // sits on the corner of the bell the way every other app does it.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    // 99+ rather than a growing number: the exact total matters far
                    // less than knowing there is something new to read.
                    text = if (unreadCount > 99) "99+" else "$unreadCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Titled group of related settings, used to give long screens some structure. */
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, modifier = Modifier.padding(horizontal = 4.dp))
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}