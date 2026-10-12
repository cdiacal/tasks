package com.cdiacal.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.BackupData
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario

/** Restoring replaces everything, so it always asks first and spells out the numbers. */
@Composable
fun RestoreDialog(data: BackupData, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.backup_restore_message, data.tasks.size, data.categories.size),
                    style = MaterialTheme.typography.bodyLarge,
                )
                data.habits?.let {
                    Text(
                        stringResource(R.string.backup_restore_habits, it.size),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            BotonPrincipal(texto = stringResource(R.string.backup_restore_confirm), onClick = onConfirm)
        },
        dismissButton = {
            BotonSecundario(texto = stringResource(R.string.cancel), onClick = onDismiss)
        },
    )
}
