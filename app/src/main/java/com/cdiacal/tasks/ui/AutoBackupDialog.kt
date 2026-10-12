package com.cdiacal.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.backup.AutoBackup
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.theme.colorGasto
import java.text.DateFormat
import java.util.Date

/** Folder, on/off switch and last-run status of the weekly automatic backup. */
@Composable
fun AutoBackupDialog(
    state: AutoBackup.State,
    running: Boolean,
    onChooseFolder: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_backup_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.auto_backup_desc), fontSize = 13.sp, color = secondary)

                Text(stringResource(R.string.auto_backup_folder), fontSize = 12.sp, color = secondary)
                Text(state.folderName ?: stringResource(R.string.auto_backup_no_folder), fontSize = 14.sp)
                BotonSecundario(texto = stringResource(R.string.auto_backup_choose), onClick = onChooseFolder)

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.auto_backup_switch), Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = state.enabled && state.hasFolder, enabled = state.hasFolder, onCheckedChange = onEnabledChange)
                }

                Text(
                    state.lastBackupAt?.let {
                        stringResource(
                            R.string.auto_backup_last,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)),
                        )
                    } ?: stringResource(R.string.auto_backup_never),
                    fontSize = 13.sp,
                    color = secondary,
                )
                state.lastError?.let {
                    Text(stringResource(R.string.auto_backup_error, it), fontSize = 13.sp, color = colorGasto())
                }
                BotonSecundario(
                    texto = stringResource(if (running) R.string.auto_backup_running else R.string.auto_backup_now),
                    onClick = onRunNow,
                    enabled = state.hasFolder && !running,
                )
            }
        },
        confirmButton = {
            BotonSecundario(texto = stringResource(R.string.close), onClick = onDismiss)
        },
    )
}
