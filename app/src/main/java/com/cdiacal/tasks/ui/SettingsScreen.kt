package com.cdiacal.tasks.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.BackupData
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.TaskSort
import com.cdiacal.tasks.habits.formatMinutes
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.components.CabeceraSimple
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import com.cdiacal.tasks.ui.theme.ThemeMode
import com.cdiacal.tasks.ui.theme.colorGasto
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Everything configurable in one place, in sections. Shown over the whole app. */
@Composable
fun SettingsScreen(viewModel: TasksViewModel, onBack: () -> Unit, onOpenCalendar: () -> Unit = {}) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val digest by viewModel.digest.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()
    val autoBackupRunning by viewModel.autoBackupRunning.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(text: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
        }
    }

    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }
    var showAutoBackup by remember { mutableStateOf(false) }
    var managingCategories by remember { mutableStateOf(false) }
    // null = closed; Category(name = "") = new category; otherwise editing.
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showDigestTime by remember { mutableStateOf(false) }
    var confirmClearDone by remember { mutableStateOf(false) }

    // Whether notifications are allowed can change in the system settings: re-check on return.
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun requestNotificationPermissionIfNeeded() {
        val missing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (missing) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Backups go through the system file picker, so no storage permission is needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.writeBackup(uri) { result ->
                showMessage(
                    result.fold(
                        onSuccess = { context.getString(R.string.backup_saved, it) },
                        onFailure = { context.getString(R.string.backup_save_failed) },
                    ),
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.readBackup(uri) { result ->
                result.fold(
                    onSuccess = { pendingRestore = it },
                    onFailure = { showMessage(context.getString(R.string.backup_invalid)) },
                )
            }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.setAutoBackupFolder(uri)
    }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    BackHandler(onBack = onBack)

    // Scaffold is a Surface: it also stops touches from reaching the screen underneath.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CabeceraSimple(titulo = stringResource(R.string.settings_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SeccionTarjeta(stringResource(R.string.section_appearance)) {
                    Etiqueta(stringResource(R.string.theme))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            Pastilla(
                                texto = stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    },
                                ),
                                activa = mode == themeMode,
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                }

                SeccionTarjeta(stringResource(R.string.section_tasks)) {
                    Etiqueta(stringResource(R.string.sort_title))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskSort.entries.forEach { option ->
                            Pastilla(
                                texto = stringResource(if (option == TaskSort.DATE) R.string.sort_date else R.string.sort_priority),
                                activa = option == state.sort,
                                onClick = { viewModel.setSort(option) },
                            )
                        }
                    }
                    Fila(
                        titulo = stringResource(R.string.categories),
                        detalle = stringResource(R.string.categories_count, state.categories.size),
                        onClick = { managingCategories = true },
                    )
                    Fila(
                        titulo = stringResource(R.string.clear_done_title),
                        colorTitulo = colorGasto(),
                        onClick = { confirmClearDone = true },
                    )
                }

                SeccionTarjeta(stringResource(R.string.section_alerts)) {
                    Fila(
                        titulo = stringResource(R.string.digest_switch),
                        detalle = stringResource(R.string.digest_desc),
                        derecha = {
                            Switch(
                                checked = digest.enabled,
                                onCheckedChange = {
                                    viewModel.setDigest(digest.copy(enabled = it))
                                    if (it) requestNotificationPermissionIfNeeded()
                                },
                            )
                        },
                    )
                    Fila(
                        titulo = stringResource(R.string.digest_time),
                        detalle = "⏰ ${formatMinutes(digest.minutes)}",
                        onClick = { showDigestTime = true },
                    )
                    Fila(
                        titulo = stringResource(R.string.notifications_title),
                        detalle = stringResource(if (notificationsOn) R.string.notifications_on else R.string.notifications_off),
                        colorDetalle = if (notificationsOn) null else colorGasto(),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        },
                    )
                }

                SeccionTarjeta(stringResource(R.string.section_backup)) {
                    Fila(
                        titulo = stringResource(R.string.backup_export),
                        onClick = { exportLauncher.launch("tareas-${LocalDate.now()}.json") },
                    )
                    Fila(
                        titulo = stringResource(R.string.backup_import),
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                    )
                    Fila(
                        titulo = stringResource(R.string.auto_backup_title),
                        detalle = if (autoBackup.enabled && autoBackup.hasFolder) {
                            stringResource(R.string.auto_backup_summary_on, autoBackup.folderName.orEmpty())
                        } else {
                            stringResource(R.string.auto_backup_summary_off)
                        },
                        onClick = { showAutoBackup = true },
                    )
                }

                SeccionTarjeta(stringResource(R.string.section_calendar)) {
                    Fila(
                        titulo = stringResource(R.string.calendar_row),
                        detalle = stringResource(R.string.calendar_row_detail),
                        onClick = onOpenCalendar,
                    )
                }

                SeccionTarjeta(stringResource(R.string.section_about)) {
                    Fila(titulo = stringResource(R.string.about_version, version))
                    Text(
                        stringResource(R.string.about_privacy),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmClearDone) {
        AlertDialog(
            onDismissRequest = { confirmClearDone = false },
            title = { Text(stringResource(R.string.clear_done_title)) },
            text = { Text(stringResource(R.string.clear_done_text, state.stats.done)) },
            confirmButton = {
                BotonPrincipal(texto = stringResource(R.string.clear_done_confirm), onClick = {
                    viewModel.clearDone()
                    confirmClearDone = false
                })
            },
            dismissButton = { BotonSecundario(texto = stringResource(R.string.cancel), onClick = { confirmClearDone = false }) },
        )
    }

    if (showDigestTime) {
        MinutesPickerDialog(
            initialMinutes = digest.minutes,
            onConfirm = {
                viewModel.setDigest(digest.copy(minutes = it))
                showDigestTime = false
            },
            onDismiss = { showDigestTime = false },
        )
    }

    if (showAutoBackup) {
        AutoBackupDialog(
            state = autoBackup,
            running = autoBackupRunning,
            onChooseFolder = { folderPicker.launch(null) },
            onEnabledChange = viewModel::setAutoBackupEnabled,
            onRunNow = viewModel::autoBackupNow,
            onDismiss = { showAutoBackup = false },
        )
    }

    pendingRestore?.let { data ->
        RestoreDialog(
            data = data,
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                viewModel.restoreBackup(data) { showMessage(context.getString(R.string.backup_restored)) }
            },
        )
    }

    if (managingCategories) {
        CategoryManagerDialog(
            categories = state.categories,
            onDismiss = { managingCategories = false },
            onNew = { editingCategory = Category(name = "") },
            onEdit = { editingCategory = it },
        )
    }

    editingCategory?.let { category ->
        CategoryEditorDialog(
            initial = category,
            onDismiss = { editingCategory = null },
            onSave = {
                viewModel.saveCategory(it)
                editingCategory = null
            },
            onDelete = {
                viewModel.deleteCategory(category)
                editingCategory = null
            },
        )
    }
}

@Composable
private fun Etiqueta(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One setting: title, optional detail line, optional control on the right; tappable when [onClick] is given. */
@Composable
private fun Fila(
    titulo: String,
    detalle: String? = null,
    colorTitulo: Color = Color.Unspecified,
    colorDetalle: Color? = null,
    onClick: (() -> Unit)? = null,
    derecha: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                titulo,
                fontSize = 14.sp,
                color = if (colorTitulo == Color.Unspecified) MaterialTheme.colorScheme.onSurface else colorTitulo,
            )
            if (detalle != null) {
                Text(detalle, fontSize = 12.sp, color = colorDetalle ?: MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        derecha?.invoke()
    }
}
