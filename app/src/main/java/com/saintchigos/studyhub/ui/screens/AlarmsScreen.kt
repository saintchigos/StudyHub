package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.data.DailyAlarm
import com.saintchigos.studyhub.ui.FocusViewModel

/**
 * Wake-up alarms on their own screen.
 *
 * These used to live at the bottom of the study timer screen, which meant tapping
 * "Alarm" on the home grid dropped you onto a countdown clock instead of your
 * alarms. A student checking tomorrow's wake-up should not have to scroll past a
 * timer they are not using.
 */
@Composable
fun AlarmsScreen(viewModel: FocusViewModel) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    var editingAlarm by remember { mutableStateOf<DailyAlarm?>(null) }
    var addingAlarm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Wake-up alarms", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            AlarmSection(
                alarms = alarms,
                onAdd = { addingAlarm = true },
                onToggle = viewModel::toggleAlarm,
                onDelete = viewModel::deleteAlarm,
                onPreview = viewModel::previewAlarm,
                onEdit = { editingAlarm = it }
            )
        }
    }

    if (addingAlarm || editingAlarm != null) {
        AlarmEditorDialog(
            existing = editingAlarm,
            onDismiss = {
                addingAlarm = false
                editingAlarm = null
            },
            onSave = { alarmLabel, minuteOfDay, mask, vibrate, sound ->
                val existing = editingAlarm
                if (existing == null) {
                    viewModel.addAlarm(alarmLabel, minuteOfDay, mask, vibrate, sound)
                } else {
                    viewModel.updateAlarm(
                        existing.copy(
                            label = alarmLabel,
                            minuteOfDay = minuteOfDay,
                            daysMask = mask,
                            vibrate = vibrate,
                            sound = sound
                        )
                    )
                }
                addingAlarm = false
                editingAlarm = null
            }
        )
    }
}