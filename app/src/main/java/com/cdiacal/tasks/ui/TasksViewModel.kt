package com.cdiacal.tasks.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.data.BackupData
import com.cdiacal.tasks.data.BackupJson
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.InvalidBackupException
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.widget.createRepository
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
    val stats: TaskStats = TaskStats(0, 0, 0, 0),
    /** Today's local date as UTC midnight millis (same unit as [Task.dueDate]). */
    val todayUtcMillis: Long = 0,
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = createRepository(application)
    private val filter = MutableStateFlow(TaskFilter.PENDING)
    private val categoryFilter = MutableStateFlow<Long?>(null)

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    val uiState: StateFlow<TasksUiState> =
        combine(repository.tasks, filter, categoryFilter, repository.categories) { all, f, categoryId, categories ->
            val today = reminderToUtcDateMillis(System.currentTimeMillis())
            TasksUiState(
                tasks = f.apply(all).inCategory(categoryId),
                filter = f,
                categories = categories,
                selectedCategoryId = categoryId,
                stats = computeStats(all, today),
                todayUtcMillis = today,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setFilter(value: TaskFilter) = filter.update { value }

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

    fun toggleDone(task: Task) = save(task.copy(isDone = !task.isDone))

    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }

    /** Used by "undo": re-inserts the exact same task (same id). */
    fun restore(task: Task) = save(task)

    fun clearDone() = viewModelScope.launch { repository.clearDone() }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    }
}
