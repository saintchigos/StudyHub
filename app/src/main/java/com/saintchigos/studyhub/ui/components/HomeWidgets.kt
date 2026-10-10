package com.saintchigos.studyhub.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.domain.StudyLevel
import com.saintchigos.studyhub.domain.TodayPlan
import com.saintchigos.studyhub.domain.WeeklyFocus

/**
 * What to do first, ranked by how soon it bites.
 *
 * An empty list is a good thing, so it says so rather than leaving a blank card.
 */
@Composable
fun TodayPlanCard(items: List<TodayPlan.Item>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "Today's plan")
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = "You are all caught up. Nothing is due in the next few days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            } else {
                items.forEachIndexed { index, item ->
                    PlanRow(item)
                    if (index != items.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanRow(item: TodayPlan.Item) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(courseColor(item.colorIndex))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        val label: String
        val tint = when (item.urgency) {
            TodayPlan.Urgency.OVERDUE -> { label = "Overdue"; scheme.error }
            TodayPlan.Urgency.TODAY -> { label = if (item.isExam) "Today" else "Due today"; scheme.tertiary }
            TodayPlan.Urgency.SOON -> { label = "Soon"; scheme.primary }
        }
        Surface(shape = RoundedCornerShape(10.dp), color = tint.copy(alpha = 0.14f)) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

/**
 * Level, title and progress, earned from time spent in focus mode.
 *
 * The bar fills with an animation when Home opens, which is a small reward for
 * looking, and tapping the card goes straight to the study timer.
 */
@Composable
fun StudyLevelCard(
    status: StudyLevel.Status,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val progress by animateFloatAsState(targetValue = status.progress, label = "levelProgress")
    val textColor = scheme.onSecondary

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.secondary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(scheme.secondary, lerp(scheme.secondary, scheme.tertiary, 0.6f))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.level.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level ${status.level}  ·  ${status.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${status.xp} XP  ·  ${status.nextLevelXp - status.xp} to level ${status.level + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Track and fill, both rounded, the fill growing to the animated fraction.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(textColor.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(textColor)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Every minute of focus time is a point.",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Seven bars, one per day, growing in when Home opens. */
@Composable
fun WeeklyFocusCard(days: List<WeeklyFocus.Day>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val total = days.sumOf { it.minutes }
    val peak = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "This week")
            Text(
                text = if (total == 0) "No focus time logged yet." else formatMinutes(total) + " of focus in the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val target = if (day.minutes == 0) 6.dp else (96f * day.minutes / peak).dp
                    val height by animateDpAsState(targetValue = target, label = "bar")
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(height)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (day.isToday) scheme.primary
                                    else if (day.minutes == 0) scheme.surfaceContainerHighest
                                    else scheme.primary.copy(alpha = 0.45f)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) scheme.primary else scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatMinutes(total: Int): String =
    if (total < 60) "$total min" else "${total / 60} h ${total % 60} min"
