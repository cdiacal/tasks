package com.cdiacal.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.habits.formatMinutes
import com.cdiacal.tasks.reminders.DigestConfig
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario

/** Switch and time of the morning summary notification. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigestDialog(config: DigestConfig, onChange: (DigestConfig) -> Unit, onDismiss: () -> Unit) {
    var showTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.digest_menu)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.digest_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.digest_switch), Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = config.enabled, onCheckedChange = { onChange(config.copy(enabled = it)) })
                }
                BotonSecundario(
                    texto = "⏰ ${formatMinutes(config.minutes)}",
                    onClick = { showTime = true },
                )
            }
        },
        confirmButton = {
            BotonSecundario(texto = stringResource(R.string.close), onClick = onDismiss)
        },
    )

    if (showTime) {
        val time = rememberTimePickerState(
            initialHour = config.minutes / 60,
            initialMinute = config.minutes % 60,
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
                        onChange(config.copy(minutes = time.hour * 60 + time.minute))
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
