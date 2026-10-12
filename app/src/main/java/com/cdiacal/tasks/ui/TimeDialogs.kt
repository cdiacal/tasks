package com.cdiacal.tasks.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cdiacal.tasks.R
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario

/** A 24-hour time picker returning minutes after midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinutesPickerDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val time = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_time)) },
        text = { TimePicker(state = time) },
        confirmButton = {
            BotonPrincipal(texto = stringResource(R.string.save), onClick = { onConfirm(time.hour * 60 + time.minute) })
        },
        dismissButton = { BotonSecundario(texto = stringResource(R.string.cancel), onClick = onDismiss) },
    )
}
