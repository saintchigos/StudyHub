package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val WheelItemHeight = 44.dp
private const val WheelVisibleItems = 5

/**
 * A drum-style wheel of values that snaps to the middle row.
 *
 * The Material clock dial is the default time picker, but on a phone it is fiddly to
 * set exactly: you have to hit the right ring and drag to the right number. A scrolling
 * wheel is what students already use for alarms and timers, and every value is one flick
 * away, so this replaces the dial everywhere a time is entered.
 *
 * Selection is read from the LazyColumn: with [WheelVisibleItems] rows of padding at each
 * end, the middle row is always `firstVisibleItemIndex + lead`.
 */
@Composable
private fun ScrollWheel(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lead = WheelVisibleItems / 2
    val lastIndex = (labels.size - 1).coerceAtLeast(0)
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - lead).coerceIn(0, lastIndex)
    )
    val fling = rememberSnapFlingBehavior(state)
    val scheme = MaterialTheme.colorScheme
    val currentSelected = rememberUpdatedState(selectedIndex)
    val currentOnSelected = rememberUpdatedState(onSelected)

    // Report the centred value as the wheel moves, and again on every settle.
    LaunchedEffect(state, labels.size) {
        snapshotFlow { state.firstVisibleItemIndex + lead }
            .collect { raw ->
                val index = raw.coerceIn(0, lastIndex)
                if (index != currentSelected.value) currentOnSelected.value(index)
            }
    }

    Box(
        modifier = modifier.height(WheelItemHeight * WheelVisibleItems),
        contentAlignment = Alignment.Center
    ) {
        // A soft band marks the row that will be chosen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primary.copy(alpha = 0.10f))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = WheelItemHeight * lead),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(labels) { index, label ->
                val isSelected = index == selectedIndex
                val distance = abs(index - selectedIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) scheme.primary else scheme.onSurface,
                        modifier = Modifier.alpha(
                            when (distance) {
                                0 -> 1f
                                1 -> 0.55f
                                2 -> 0.30f
                                else -> 0.16f
                            }
                        )
                    )
                }
            }
        }
    }
}

/**
 * Scrolling hour/minute wheels in a dialog, with an optional AM/PM wheel.
 *
 * [initialHour] is always the stored 24-hour value; when [is24Hour] is false the hour
 * wheel shows 1-12 and the AM/PM wheel decides the displayed half of the day.
 */
@Composable
fun TimeScrollDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    is24Hour: Boolean = true
) {
    val useAmPm = !is24Hour
    val safeHour = initialHour.coerceIn(0, 23)
    val safeMinute = initialMinute.coerceIn(0, 59)

    var hour12 by remember { mutableIntStateOf(if (safeHour % 12 == 0) 12 else safeHour % 12) }
    var hour24 by remember { mutableIntStateOf(safeHour) }
    var minute by remember { mutableIntStateOf(safeMinute) }
    var amPm by remember { mutableIntStateOf(if (safeHour < 12) 0 else 1) }

    val hours = if (useAmPm) (1..12).map { it.toString() }
    else (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollWheel(
                    labels = hours,
                    selectedIndex = if (useAmPm) hour12 - 1 else hour24,
                    onSelected = { if (useAmPm) hour12 = it + 1 else hour24 = it },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                ScrollWheel(
                    labels = minutes,
                    selectedIndex = minute,
                    onSelected = { minute = it },
                    modifier = Modifier.weight(1f)
                )
                if (useAmPm) {
                    ScrollWheel(
                        labels = listOf("AM", "PM"),
                        selectedIndex = amPm,
                        onSelected = { amPm = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .width(64.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = if (useAmPm) {
                        val base = hour12 % 12
                        if (amPm == 1) base + 12 else base
                    } else {
                        hour24
                    }
                    onConfirm(hour, minute)
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
