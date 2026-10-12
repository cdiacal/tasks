package com.cdiacal.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.theme.colorGasto
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/** The date picker works in UTC midnight millis, so format in UTC to avoid off-by-one days. */
fun formatDate(utcMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(utcMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    initial: Task,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Task) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var dueDate by remember { mutableStateOf(initial.dueDate) }
    var reminderAt by remember { mutableStateOf(initial.reminderAt) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Reminder is picked in two steps: date first, then time. Holds the chosen date meanwhile.
    var pendingReminderDate by remember { mutableStateOf<Long?>(null) }
    var showReminderDate by remember { mutableStateOf(false) }

    val isNew = initial.id == 0L && initial.title.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_task else R.string.edit_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotonSecundario(
                        texto = dueDate?.let(::formatDate) ?: stringResource(R.string.due_date),
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) {
                            Text(stringResource(R.string.no_due_date))
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotonSecundario(
                        texto = reminderAt?.let(::formatReminder) ?: stringResource(R.string.reminder),
                        onClick = { showReminderDate = true },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (reminderAt != null) {
                        TextButton(onClick = { reminderAt = null }) {
                            Text(stringResource(R.string.no_reminder))
                        }
                    }
                }
                if (reminderAt != null && reminderAt!! <= System.currentTimeMillis()) {
                    Text(
                        stringResource(R.string.reminder_in_past),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorGasto(),
                    )
                }
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = colorGasto())
                    }
                }
            }
        },
        confirmButton = {
            BotonPrincipal(
                texto = stringResource(R.string.save),
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            dueDate = dueDate,
                            reminderAt = reminderAt,
                        ),
                    )
                },
            )
        },
        dismissButton = {
            BotonSecundario(texto = stringResource(R.string.cancel), onClick = onDismiss)
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = pickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    // Step 1 of the reminder: pick the date.
    if (showReminderDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = reminderAt?.let(::reminderToUtcDateMillis)
                ?: dueDate
                ?: reminderToUtcDateMillis(System.currentTimeMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDate = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        pendingReminderDate = state.selectedDateMillis
                        showReminderDate = false
                    },
                ) { Text(stringResource(R.string.next)) }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDate = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    // Step 2: pick the time, then combine with the chosen date.
    pendingReminderDate?.let { date ->
        val time = rememberTimePickerState(
            initialHour = reminderAt?.let(::reminderHour) ?: 9,
            initialMinute = reminderAt?.let(::reminderMinute) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pendingReminderDate = null },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = time) },
            confirmButton = {
                TextButton(onClick = {
                    reminderAt = combineReminder(date, time.hour, time.minute)
                    pendingReminderDate = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingReminderDate = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
