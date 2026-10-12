package com.cdiacal.tasks.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.Priority
import com.cdiacal.tasks.data.Recurrence
import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskSort
import com.cdiacal.tasks.data.allDone
import com.cdiacal.tasks.data.priorityKind
import com.cdiacal.tasks.data.recurrenceKind
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.CabeceraHero
import com.cdiacal.tasks.ui.components.CifraHero
import com.cdiacal.tasks.ui.components.FilaPestanas
import com.cdiacal.tasks.ui.components.IconoInicial
import com.cdiacal.tasks.ui.components.MiniDatoHero
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.components.TarjetaPanel
import com.cdiacal.tasks.ui.theme.colorAviso
import com.cdiacal.tasks.ui.theme.colorGasto
import com.cdiacal.tasks.ui.theme.colorIngreso
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel(),
    newTaskTrigger: Int = 0,
    onNewTaskConsumed: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** Lets the host hide its bottom bar while the full-screen editor is open. */
    onEditorVisibleChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // null = editor closed; Task(title = "") = new task; otherwise editing.
    var editing by remember { mutableStateOf<Task?>(null) }
    // The home-screen widget's "+" button asks the app to open the new-task editor.
    LaunchedEffect(newTaskTrigger) {
        if (newTaskTrigger > 0) {
            editing = Task(title = "")
            onNewTaskConsumed()
        }
    }
    LaunchedEffect(editing != null) { onEditorVisibleChange(editing != null) }
    DisposableEffect(Unit) { onDispose { onEditorVisibleChange(false) } }

    var searching by rememberSaveable { mutableStateOf(false) }
    val expandedIds by viewModel.expandedTaskIds.collectAsStateWithLifecycle()
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* If denied, reminders are saved but no notification is shown. */ }

    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.undo)

    fun showMessage(text: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
        }
    }

    fun deleteWithUndo(task: Task) {
        val steps = state.subtasksByTask[task.id].orEmpty() // kept so "undo" brings them back too
        viewModel.delete(task)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(task, steps)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                BotonPrincipal(
                    texto = stringResource(R.string.add_task),
                    icono = Icons.Default.Add,
                    onClick = { editing = Task(title = "") },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Hero(state = state, onSearch = { searching = true }, onSettings = onOpenSettings)

                if (searching) {
                    SearchBar(
                        query = state.query,
                        onQuery = viewModel::setQuery,
                        onClose = {
                            searching = false
                            viewModel.setQuery("")
                        },
                    )
                }

                val filters = TaskFilter.entries
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilaPestanas(
                        etiquetas = filters.map {
                            when (it) {
                                TaskFilter.PENDING -> stringResource(R.string.filter_pending)
                                TaskFilter.DONE -> stringResource(R.string.filter_done)
                                TaskFilter.ALL -> stringResource(R.string.filter_all)
                            }
                        },
                        seleccionada = filters.indexOf(state.filter),
                        onSelect = { viewModel.setFilter(filters[it]) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    )
                    // Always visible, one tap: switches the list between date and priority order.
                    val byPriority = state.sort == TaskSort.PRIORITY
                    Pastilla(
                        texto = "↕ " + stringResource(if (byPriority) R.string.sort_priority else R.string.sort_date),
                        activa = byPriority,
                        onClick = { viewModel.setSort(if (byPriority) TaskSort.DATE else TaskSort.PRIORITY) },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }

                if (state.categories.isNotEmpty()) {
                    val selectedIndex = state.categories.indexOfFirst { it.id == state.selectedCategoryId } + 1
                    FilaPestanas(
                        etiquetas = listOf(stringResource(R.string.all_categories)) + state.categories.map { c ->
                            listOf(c.emoji, c.name).filter { it.isNotBlank() }.joinToString(" ")
                        },
                        seleccionada = selectedIndex,
                        onSelect = { viewModel.setCategoryFilter(state.categories.getOrNull(it - 1)?.id) },
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }

                if (state.tasks.isEmpty()) {
                    EmptyState(state.filter, state.query)
                } else {
                    // Ordered by priority, the list gets a header per level; by date, it is one flat list.
                    val groups = remember(state.tasks, state.sort) {
                        if (state.sort == TaskSort.PRIORITY) {
                            priorityGroups(state.tasks)
                        } else {
                            emptyList()
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 128.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val card: @Composable (Task) -> Unit = { task ->
                            TaskCard(
                                task = task,
                                todayUtcMillis = state.todayUtcMillis,
                                category = task.categoryId?.let(categoriesById::get),
                                steps = state.subtasksByTask[task.id].orEmpty(),
                                expanded = task.id in expandedIds,
                                onToggleExpanded = { viewModel.toggleExpanded(task.id) },
                                onToggleStep = { step ->
                                    viewModel.toggleStep(step) { next ->
                                        showMessage(context.getString(R.string.task_repeats_next, formatDate(next)))
                                    }
                                },
                                onToggle = {
                                    viewModel.toggleDone(task) { next ->
                                        showMessage(context.getString(R.string.task_repeats_next, formatDate(next)))
                                    }
                                },
                                onClick = { editing = task },
                            )
                        }
                        if (groups.isEmpty()) {
                            items(state.tasks, key = { it.id }) { task -> card(task) }
                        } else {
                            groups.forEach { group ->
                                item(key = "header-${group.key}") { GroupHeader(group.key) }
                                items(group.tasks, key = { it.id }) { task -> card(task) }
                            }
                        }
                    }
                }
            }
        }

        editing?.let { task ->
            TaskEditorScreen(
                initial = task,
                initialSteps = state.subtasksByTask[task.id].orEmpty(),
                categories = state.categories,
                onDismiss = { editing = null },
                onDelete = {
                    editing = null
                    deleteWithUndo(task)
                },
                onSave = { saved, steps ->
                    // Complete the task only when this edit is what finished the checklist.
                    val finishedNow = steps.allDone() && !state.subtasksByTask[task.id].orEmpty().allDone()
                    viewModel.saveWithSteps(saved, steps, completeIfAllStepsDone = finishedNow) { next ->
                        showMessage(context.getString(R.string.task_repeats_next, formatDate(next)))
                    }
                    editing = null
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (saved.reminderAt != null && needsPermission) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
    }
}

@Composable
private fun Hero(state: TasksUiState, onSearch: () -> Unit, onSettings: () -> Unit) {
    val today = remember { DateFormat.getDateInstance(DateFormat.LONG).format(Date()) }
    CabeceraHero {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(today, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = Color.White)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color.White)
                }
            }
        }
        Text(
            stringResource(R.string.hero_pending_label),
            modifier = Modifier.padding(top = 18.dp),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.5.sp,
        )
        CifraHero(state.stats.pending.toString())
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiniDatoHero(stringResource(R.string.stat_today), state.stats.dueToday.toString(), Modifier.weight(1f))
            MiniDatoHero(stringResource(R.string.stat_overdue), state.stats.overdue.toString(), Modifier.weight(1f))
            MiniDatoHero(stringResource(R.string.stat_done), state.stats.done.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun GroupHeader(key: GroupKey) {
    Text(
        stringResource(
            when (key) {
                GroupKey.HIGH -> R.string.group_high
                GroupKey.MEDIUM -> R.string.group_medium
                GroupKey.LOW -> R.string.group_low
                GroupKey.NONE -> R.string.group_none
                GroupKey.DONE -> R.string.group_done
            },
        ),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyState(filter: TaskFilter, query: String) {
    val text = if (query.isNotBlank()) R.string.search_no_results else when (filter) {
        TaskFilter.PENDING -> R.string.empty_pending
        TaskFilter.DONE -> R.string.empty_done
        TaskFilter.ALL -> R.string.empty_all
    }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (query.isNotBlank()) stringResource(text, query.trim()) else stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).focusRequester(focus),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            shape = RoundedCornerShape(12.dp),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_close))
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    todayUtcMillis: Long,
    category: Category?,
    steps: List<Subtask>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleStep: (Subtask) -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val kind = task.recurrenceKind()
    val repeatLabel = if (kind == Recurrence.NONE) null else recurrenceLabel(kind)
    val dueColor = when (dueState(task, todayUtcMillis)) {
        DueState.OVERDUE -> colorGasto()
        DueState.SOON -> colorAviso()
        DueState.NONE -> secondary
    }
    val checkColors = CheckboxDefaults.colors(
        checkedColor = colorIngreso(),
        checkmarkColor = MaterialTheme.colorScheme.background,
        uncheckedColor = secondary,
    )
    TarjetaPanel(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconoInicial(
                    inicial = (category?.name ?: task.title).take(1).uppercase(),
                    id = category?.id ?: task.id,
                    emoji = category?.emoji,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (task.isDone) secondary else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    )
                    category?.let {
                        Text(it.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                    val priority = task.priorityKind()
                    if (priority != Priority.NONE) {
                        Text(
                            "${priorityMarker(priority)} ${priorityLabel(priority)}",
                            fontSize = 12.sp,
                            fontWeight = if (priority == Priority.HIGH) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (priority == Priority.LOW) secondary else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                    task.dueDate?.let {
                        Text(
                            formatDate(it) + (repeatLabel?.let { label -> " · 🔁 $label" } ?: ""),
                            fontSize = 12.sp,
                            color = dueColor,
                            maxLines = 1,
                        )
                    }
                    if (steps.isNotEmpty()) {
                        // Tapping the progress line folds / unfolds the checklist.
                        Row(Modifier.clickable(onClick = onToggleExpanded), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.steps_progress, steps.count { it.isDone }, steps.size),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(if (expanded) R.string.steps_hide else R.string.steps_show),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    task.reminderAt?.let {
                        Text("🔔 ${formatReminder(it)}", fontSize = 12.sp, color = secondary, maxLines = 1)
                    }
                    if (task.notes.isNotBlank()) {
                        Text(task.notes, fontSize = 12.sp, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Checkbox(checked = task.isDone, onCheckedChange = { onToggle() }, colors = checkColors)
            }
            if (expanded && steps.isNotEmpty()) {
                Column(Modifier.padding(start = 48.dp)) {
                    steps.forEach { step ->
                        Row(Modifier.fillMaxWidth().clickable { onToggleStep(step) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = step.isDone, onCheckedChange = { onToggleStep(step) }, colors = checkColors)
                            Text(
                                step.title,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = if (step.isDone) secondary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (step.isDone) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
