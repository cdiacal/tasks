package com.cdiacal.tasks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.Priority
import com.cdiacal.tasks.data.Recurrence
import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.components.CabeceraSimple
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.components.PastillaColor
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import com.cdiacal.tasks.ui.theme.colorAviso
import com.cdiacal.tasks.ui.theme.colorGasto
import com.cdiacal.tasks.ui.theme.colorIngreso
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

@Composable
fun priorityLabel(level: Priority): String = stringResource(
    when (level) {
        Priority.NONE -> R.string.priority_none
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    },
)

/** Traffic-light colour of a priority (green low, amber medium, red high); null for none. */
@Composable
fun priorityColor(level: Priority): Color? = when (level) {
    Priority.NONE -> null
    Priority.LOW -> colorIngreso()
    Priority.MEDIUM -> colorAviso()
    Priority.HIGH -> colorGasto()
}

/** "Prioridad alta" etc.; empty for no priority. */
@Composable
fun priorityTag(level: Priority): String = when (level) {
    Priority.NONE -> ""
    Priority.LOW -> stringResource(R.string.group_low)
    Priority.MEDIUM -> stringResource(R.string.group_medium)
    Priority.HIGH -> stringResource(R.string.group_high)
}

@Composable
fun recurrenceLabel(kind: Recurrence): String = stringResource(
    when (kind) {
        Recurrence.NONE -> R.string.repeat_none
        Recurrence.DAILY -> R.string.repeat_daily
        Recurrence.WEEKLY -> R.string.repeat_weekly
        Recurrence.MONTHLY -> R.string.repeat_monthly
        Recurrence.YEARLY -> R.string.repeat_yearly
    },
)

/** The date picker works in UTC midnight millis, so format in UTC to avoid off-by-one days. */
fun formatDate(utcMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(utcMillis))

/**
 * Full-screen task editor, in sections: the task itself, when (date, repeat, reminder), how to
 * organize it (category, priority) and its steps. Back / Cancel ask before discarding changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    initial: Task,
    initialSteps: List<Subtask>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Task, List<Subtask>) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var dueDate by remember { mutableStateOf(initial.dueDate) }
    var reminderAt by remember { mutableStateOf(initial.reminderAt) }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var recurrence by remember { mutableStateOf(initial.recurrence) }
    var priority by remember { mutableStateOf(initial.priority) }
    val steps = remember { mutableStateListOf<Subtask>().apply { addAll(initialSteps) } }
    var newStep by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    // Reminder is picked in two steps: date first, then time. Holds the chosen date meanwhile.
    var pendingReminderDate by remember { mutableStateOf<Long?>(null) }
    var showReminderDate by remember { mutableStateOf(false) }

    val todayUtc = remember { reminderToUtcDateMillis(System.currentTimeMillis()) }
    val isNew = initial.id == 0L
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (isNew) titleFocus.requestFocus() }

    fun addStep() {
        val text = newStep.trim()
        if (text.isNotEmpty()) {
            steps.add(Subtask(taskId = initial.id, title = text))
            newStep = ""
        }
    }

    val dirty = title != initial.title || notes != initial.notes || dueDate != initial.dueDate ||
        reminderAt != initial.reminderAt || categoryId != initial.categoryId ||
        recurrence != initial.recurrence || priority != initial.priority ||
        steps.toList() != initialSteps || newStep.isNotBlank()

    fun requestClose() {
        if (dirty) showDiscard = true else onDismiss()
    }

    fun save() {
        // A step typed but not yet added with "+" is kept rather than silently lost.
        val pending = newStep.trim()
        val finalSteps = if (pending.isEmpty()) steps.toList() else steps + Subtask(taskId = initial.id, title = pending)
        onSave(
            initial.copy(
                title = title.trim(),
                notes = notes.trim(),
                dueDate = dueDate,
                reminderAt = reminderAt,
                categoryId = categoryId,
                recurrence = if (dueDate == null) Recurrence.NONE.name else recurrence,
                priority = priority,
            ),
            finalSteps,
        )
    }

    BackHandler { requestClose() }

    // Surface: also stops touches from reaching the list underneath.
    Surface(Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            CabeceraSimple(
                titulo = stringResource(if (isNew) R.string.add_task else R.string.edit_task),
                onBack = { requestClose() },
            ) {
                if (!isNew) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SeccionTarjeta(titulo = null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.notes_label)) },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SeccionTarjeta(titulo = stringResource(R.string.editor_section_when)) {
                    Label(stringResource(R.string.due_date))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf(todayUtc, todayUtc + DAY_MILLIS, todayUtc + 7 * DAY_MILLIS)
                        Pastilla(stringResource(R.string.no_due_date), dueDate == null, {
                            dueDate = null
                            recurrence = Recurrence.NONE.name // a repeating task needs a date to repeat from
                        })
                        Pastilla(stringResource(R.string.date_today), dueDate == presets[0], { dueDate = presets[0] })
                        Pastilla(stringResource(R.string.date_tomorrow), dueDate == presets[1], { dueDate = presets[1] })
                        Pastilla(stringResource(R.string.date_week), dueDate == presets[2], { dueDate = presets[2] })
                        Pastilla(
                            "📅 " + stringResource(R.string.date_pick),
                            dueDate != null && dueDate !in presets,
                            { showDatePicker = true },
                        )
                    }
                    dueDate?.let { Text(stringResource(R.string.due_value, formatDate(it)), fontSize = 13.sp) }

                    Label(stringResource(R.string.repeat_label))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Recurrence.entries.forEach { kind ->
                            Pastilla(recurrenceLabel(kind), recurrence == kind.name, {
                                recurrence = kind.name
                                // Repeating needs an anchor date: default to today.
                                if (kind != Recurrence.NONE && dueDate == null) dueDate = todayUtc
                            })
                        }
                    }

                    Label(stringResource(R.string.reminder))
                    Text(
                        reminderAt?.let { "🔔 ${formatReminder(it)}" } ?: stringResource(R.string.reminder_none),
                        fontSize = 14.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BotonSecundario(
                            texto = stringResource(if (reminderAt == null) R.string.reminder_add else R.string.reminder_change),
                            onClick = { showReminderDate = true },
                        )
                        if (reminderAt != null) {
                            TextButton(onClick = { reminderAt = null }) { Text(stringResource(R.string.no_reminder)) }
                        }
                    }
                    if (reminderAt != null && reminderAt!! <= System.currentTimeMillis()) {
                        Text(
                            stringResource(R.string.reminder_in_past),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorGasto(),
                        )
                    }
                }

                SeccionTarjeta(titulo = stringResource(R.string.editor_section_organize)) {
                    if (categories.isNotEmpty()) {
                        Label(stringResource(R.string.category))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pastilla(stringResource(R.string.no_category), categoryId == null, { categoryId = null })
                            categories.forEach { c ->
                                Pastilla(
                                    texto = listOf(c.emoji, c.name).filter { it.isNotBlank() }.joinToString(" "),
                                    activa = categoryId == c.id,
                                    onClick = { categoryId = c.id },
                                )
                            }
                        }
                    }
                    Label(stringResource(R.string.priority))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.entries.forEach { level ->
                            PastillaColor(
                                texto = priorityLabel(level),
                                activa = priority == level.level,
                                color = priorityColor(level),
                                onClick = { priority = level.level },
                            )
                        }
                    }
                }

                SeccionTarjeta(titulo = stringResource(R.string.steps)) {
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = step.isDone, onCheckedChange = { steps[index] = step.copy(isDone = it) })
                            Text(
                                step.title,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                color = if (step.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (step.isDone) TextDecoration.LineThrough else null,
                            )
                            IconButton(onClick = { steps.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.step_remove))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newStep,
                            onValueChange = { newStep = it },
                            placeholder = { Text(stringResource(R.string.step_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addStep() }),
                        )
                        IconButton(onClick = { addStep() }, enabled = newStep.isNotBlank()) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.step_add))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BotonSecundario(
                    texto = stringResource(R.string.cancel),
                    onClick = { requestClose() },
                    modifier = Modifier.weight(1f),
                )
                BotonPrincipal(
                    texto = stringResource(R.string.save),
                    enabled = title.isNotBlank(),
                    onClick = { save() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_text)) },
            confirmButton = { BotonPrincipal(texto = stringResource(R.string.discard_confirm), onClick = onDismiss) },
            dismissButton = {
                BotonSecundario(texto = stringResource(R.string.discard_keep), onClick = { showDiscard = false })
            },
        )
    }

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
            initialSelectedDateMillis = reminderAt?.let(::reminderToUtcDateMillis) ?: dueDate ?: todayUtc,
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

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
