package com.cdiacal.tasks.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.backup.AutoBackup
import com.cdiacal.tasks.data.BackupData
import com.cdiacal.tasks.data.BackupJson
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.InvalidBackupException
import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.TaskSort
import com.cdiacal.tasks.data.TaskSortStore
import com.cdiacal.tasks.data.sortedFor
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.widget.createRepository
import com.cdiacal.tasks.reminders.DigestConfig
import com.cdiacal.tasks.reminders.DigestScheduler
import com.cdiacal.tasks.reminders.DigestSettings
import com.cdiacal.tasks.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.PENDING,
    val categories: List<Category> = emptyList(),
    /** Category used to filter the list, or null for all. */
    val selectedCategoryId: Long? = null,
    val query: String = "",
    val sort: TaskSort = TaskSort.DATE,
    /** Checklist steps of every task, keyed by task id. */
    val subtasksByTask: Map<Long, List<Subtask>> = emptyMap(),
    val stats: TaskStats = TaskStats(0, 0, 0, 0),
    /** Today's local date as UTC midnight millis (same unit as [Task.dueDate]). */
    val todayUtcMillis: Long = 0,
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = createRepository(application)
    private val filter = MutableStateFlow(TaskFilter.PENDING)
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    private val expanded = MutableStateFlow<Set<Long>>(emptySet())

    /** Tasks whose steps are unfolded in the list (kept here so it survives tab changes). */
    val expandedTaskIds: StateFlow<Set<Long>> = expanded

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private data class Source(val tasks: List<Task>, val categories: List<Category>, val subtasks: List<Subtask>)

    private val source = combine(repository.tasks, repository.categories, repository.subtasks) { t, c, s ->
        Source(t, c, s)
    }

    val uiState: StateFlow<TasksUiState> =
        combine(source, filter, categoryFilter, query, TaskSortStore.flow(application)) { src, f, categoryId, q, sort ->
            val today = reminderToUtcDateMillis(System.currentTimeMillis())
            val categoryNames = src.categories.associate { it.id to it.name }
            val steps = src.subtasks.groupBy { it.taskId }
            val visible = f.apply(src.tasks).inCategory(categoryId).filter { task ->
                matchesQuery(
                    q,
                    task.title,
                    task.notes,
                    task.categoryId?.let(categoryNames::get).orEmpty(),
                    *steps[task.id].orEmpty().map { it.title }.toTypedArray(),
                )
            }.sortedFor(sort)
            TasksUiState(
                tasks = visible,
                sort = sort,
                query = q,
                filter = f,
                categories = src.categories,
                subtasksByTask = steps,
                selectedCategoryId = categoryId,
                stats = computeStats(src.tasks, today),
                todayUtcMillis = today,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    private val _digest = MutableStateFlow(DigestSettings.load(application))
    val digest: StateFlow<DigestConfig> = _digest

    fun setDigest(config: DigestConfig) {
        DigestSettings.save(getApplication(), config)
        DigestScheduler(getApplication()).sync()
        _digest.value = config
    }

    private val _autoBackup = MutableStateFlow(AutoBackup.state(application))
    val autoBackup: StateFlow<AutoBackup.State> = _autoBackup
    private val _autoBackupRunning = MutableStateFlow(false)
    val autoBackupRunning: StateFlow<Boolean> = _autoBackupRunning

    private fun refreshAutoBackup() {
        _autoBackup.value = AutoBackup.state(getApplication())
    }

    private suspend fun runAutoBackup() {
        _autoBackupRunning.value = true
        AutoBackup.runNow(getApplication())
        _autoBackupRunning.value = false
        refreshAutoBackup()
    }

    /** Stores the picked folder, turns the weekly backup on and makes a first copy right away. */
    fun setAutoBackupFolder(uri: Uri) = viewModelScope.launch {
        AutoBackup.setFolder(getApplication(), uri)
        refreshAutoBackup()
        runAutoBackup()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        AutoBackup.setEnabled(getApplication(), enabled)
        refreshAutoBackup()
    }

    fun autoBackupNow() = viewModelScope.launch { runAutoBackup() }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setFilter(value: TaskFilter) = filter.update { value }

    fun setSort(sort: TaskSort) = TaskSortStore.set(getApplication(), sort)

    fun setQuery(value: String) = query.update { value }

    /** Writes the backup JSON to [uri] (a document the user picked). Reports the task count. */
    fun writeBackup(uri: Uri, onResult: (Result<Int>) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val (json, count) = repository.exportBackup()
            withContext(Dispatchers.IO) {
                val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: error("Cannot open $uri")
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            count
        }
        onResult(result)
    }

    /** Reads and validates a backup file without applying it. */
    fun readBackup(uri: Uri, onResult: (Result<BackupData>) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() } ?: error("Cannot open $uri")
                if (bytes.size > MAX_BACKUP_BYTES) throw InvalidBackupException("Backup too large")
                BackupJson.decode(String(bytes, Charsets.UTF_8))
            }
        }
        onResult(result)
    }

    fun restoreBackup(data: BackupData, onDone: () -> Unit) = viewModelScope.launch {
        repository.restoreBackup(data)
        categoryFilter.value = null // the selected category may not exist in the backup
        onDone()
    }

    fun setCategoryFilter(id: Long?) = categoryFilter.update { id }

    fun saveCategory(category: Category) = viewModelScope.launch { repository.saveCategory(category) }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
        if (categoryFilter.value == category.id) categoryFilter.value = null
    }

    fun save(task: Task) = viewModelScope.launch { repository.save(task) }

    fun toggleExpanded(taskId: Long) = expanded.update { if (taskId in it) it - taskId else it + taskId }

    /** [onRescheduled] gets the next due date when ticking this step completed a repeating task. */
    fun toggleStep(step: Subtask, onRescheduled: (Long) -> Unit = {}) = viewModelScope.launch {
        repository.toggleStep(step.id)?.let(onRescheduled)
    }

    fun saveWithSteps(
        task: Task,
        steps: List<Subtask>,
        completeIfAllStepsDone: Boolean = false,
        onRescheduled: (Long) -> Unit = {},
    ) = viewModelScope.launch {
        repository.saveWithSubtasks(task, steps, completeIfAllStepsDone)?.let(onRescheduled)
    }

    /** Completes/reopens a task. For a repeating task, [onRescheduled] gets its next due date. */
    fun toggleDone(task: Task, onRescheduled: (Long) -> Unit = {}) = viewModelScope.launch {
        repository.toggleDone(task)?.let(onRescheduled)
    }

    /** Completes an open task; [onDone] gets the next due date of a repeating task, else null. */
    fun complete(task: Task, onDone: (Long?) -> Unit) = viewModelScope.launch {
        onDone(repository.toggleDone(task))
    }

    fun undoCompletion(original: Task, steps: List<Subtask>) = viewModelScope.launch {
        repository.undoCompletion(original, steps)
    }

    /** Creates several tasks at once (the calendar import). */
    fun importTasks(tasks: List<Task>) = viewModelScope.launch {
        tasks.forEach { repository.save(it) }
    }

    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }

    /** Used by "undo": re-inserts the exact same task (same id). */
    fun restore(task: Task, steps: List<Subtask> = emptyList()) = saveWithSteps(task, steps)

    fun clearDone() = viewModelScope.launch { repository.clearDone() }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    }
}
