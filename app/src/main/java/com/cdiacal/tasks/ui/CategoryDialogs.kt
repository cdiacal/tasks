package com.cdiacal.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.components.IconoInicial
import com.cdiacal.tasks.ui.components.TarjetaPanel
import com.cdiacal.tasks.ui.theme.colorGasto

/** Lists all categories; tapping one edits it. */
@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Category) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (categories.isEmpty()) {
                    Text(
                        stringResource(R.string.no_categories_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                categories.forEach { category ->
                    TarjetaPanel(Modifier.fillMaxWidth(), onClick = { onEdit(category) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconoInicial(
                                inicial = category.name.take(1).uppercase(),
                                id = category.id,
                                emoji = category.emoji,
                            )
                            Text(category.name, fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
                BotonPrincipal(
                    texto = stringResource(R.string.new_category),
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            BotonSecundario(texto = stringResource(R.string.close), onClick = onDismiss)
        },
    )
}

/** Create ([initial] has id 0) or edit a category. */
@Composable
fun CategoryEditorDialog(
    initial: Category,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.new_category else R.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
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
                if (!isNew) {
                    Text(
                        stringResource(R.string.delete_category_hint),
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
                enabled = name.isNotBlank(),
                onClick = { onSave(initial.copy(name = name.trim(), emoji = emoji.trim())) },
            )
        },
        dismissButton = {
            BotonSecundario(texto = stringResource(R.string.cancel), onClick = onDismiss)
        },
    )
}
