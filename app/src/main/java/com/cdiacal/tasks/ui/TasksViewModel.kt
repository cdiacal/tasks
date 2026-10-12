package com.cdiacal.tasks.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.data.TaskRepository
import com.cdiacal.tasks.reminders.ReminderScheduler
import com.cdiacal.tasks.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.PENDING,
    val stats: TaskStats = TaskStats(0, 0, 0, 0),
    /** Today's local date as UTC midnight millis (same unit as [Task.dueDate]). */
    val todayUtcMillis: Long = 0,
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(
        TaskDatabase.get(application).taskDao(),
        ReminderScheduler(application),
    )
    private val filter = MutableStateFlow(TaskFilter.PENDING)

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    val uiState: StateFlow<TasksUiState> =
        combine(repository.tasks, filter) { all, f ->
            val today = reminderToUtcDateMillis(System.currentTimeMillis())
            TasksUiState(
                tasks = f.apply(all),
                filter = f,
                stats = computeStats(all, today),
                todayUtcMillis = today,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setFilter(value: TaskFilter) = filter.update { value }

    fun save(task: Task) = viewModelScope.launch { repository.save(task) }

    fun toggleDone(task: Task) = save(task.copy(isDone = !task.isDone))

    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }

    /** Used by "undo": re-inserts the exact same task (same id). */
    fun restore(task: Task) = save(task)

    fun clearDone() = viewModelScope.launch { repository.clearDone() }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
