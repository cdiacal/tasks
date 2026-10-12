package com.cdiacal.tasks.ui.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.formatMinutes
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.theme.colorGasto
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private fun dayLabel(index: Int): String =
    DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }

/** Create ([initial] has id 0) or edit a habit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorDialog(
    initial: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var mask by remember { mutableStateOf(initial.daysMask) }
    var reminder by remember { mutableStateOf(initial.reminderMinutes) }
    var showTime by remember { mutableStateOf(false) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.new_habit else R.string.edit_habit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(8) },
                    label = { Text(stringResource(R.string.category_emoji)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.habit_days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (i in 0..6) {
                        Pastilla(
                            texto = dayLabel(i),
                            activa = mask and (1 shl i) != 0,
                            onClick = { mask = mask xor (1 shl i) },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotonSecundario(
                        texto = reminder?.let { "⏰ ${formatMinutes(it)}" } ?: stringResource(R.string.habit_daily_reminder),
                        onClick = { showTime = true },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (reminder != null) {
                        TextButton(onClick = { reminder = null }) { Text(stringResource(R.string.no_reminder)) }
                    }
                }
                if (!isNew) {
                    Text(
                        stringResource(R.string.delete_habit_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = colorGasto())
                    }
                }
            }
        },
        confirmButton = {
            BotonPrincipal(
                texto = stringResource(R.string.save),
                // A habit needs a name and at least one scheduled weekday.
                enabled = name.isNotBlank() && mask != 0,
                onClick = { onSave(
                        initial.copy(name = name.trim(), emoji = emoji.trim(), daysMask = mask, reminderMinutes = reminder),
                    ) },
            )
        },
        dismissButton = {
            BotonSecundario(texto = stringResource(R.string.cancel), onClick = onDismiss)
        },
    )

    if (showTime) {
        val time = rememberTimePickerState(
            initialHour = (reminder ?: DEFAULT_REMINDER) / 60,
            initialMinute = (reminder ?: DEFAULT_REMINDER) % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = time) },
            confirmButton = {
                BotonPrincipal(
                    texto = stringResource(R.string.save),
                    onClick = {
                        reminder = time.hour * 60 + time.minute
                        showTime = false
                    },
                )
            },
            dismissButton = {
                BotonSecundario(texto = stringResource(R.string.cancel), onClick = { showTime = false })
            },
        )
    }
}

private const val DEFAULT_REMINDER = 9 * 60 // 09:00
