@file:OptIn(ExperimentalMaterial3Api::class)

package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.util.TimeUtil
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.US)
private val displayTime = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/**
 * rememberDatePickerState works in UTC midnight, so selections must be read back
 * as UTC too or the date can shift by one day.
 */
private fun utcMillisFor(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun localDateFromUtcMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    earliest: LocalDate? = null,
    label: String = "Date"
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date.format(displayDate),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisFor(date)
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            var picked = localDateFromUtcMillis(millis)
                            // Material 1.3.1's selectableDates parameter will not bind here,
                            // so past dates are clamped on confirm instead of disabled.
                            if (earliest != null && picked.isBefore(earliest)) picked = earliest
                            onDateChange(picked)
                        }
                        open = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = state,
                title = {
                    Text(
                        text = "Select $label",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time",
    is24Hour: Boolean = true
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time.format(displayTime),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (open) {
        TimeScrollDialog(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = is24Hour,
            onDismiss = { open = false },
            onConfirm = { hour, minute ->
                onTimeChange(LocalTime.of(hour, minute))
                open = false
            }
        )
    }
}

/** Convenience pair for the common "pick a date and a time" case. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeFields(
    date: LocalDate,
    time: LocalTime,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    earliest: LocalDate? = null,
    dateLabel: String = "Date",
    is24Hour: Boolean = true
) {
    val today = TimeUtil.now().toLocalDate()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DatePickerField(
                date = date,
                onDateChange = onDateChange,
                modifier = Modifier.weight(1f),
                earliest = earliest
            )
            TimePickerField(
                time = time,
                onTimeChange = onTimeChange,
                modifier = Modifier.weight(1f),
                is24Hour = is24Hour
            )
        }
        Spacer(Modifier.height(6.dp))
        val delta = date.toEpochDay() - today.toEpochDay()
        Text(
            text = when {
                delta < 0L -> "In the past"
                delta == 0L -> "Today"
                else -> "In $delta day${if (delta == 1L) "" else "s"}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (delta < 0L) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}