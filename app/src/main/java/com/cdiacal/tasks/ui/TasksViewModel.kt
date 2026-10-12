package com.cdiacal.tasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.data.TaskRepository
import com.cdiacal.tasks.reminders.ReminderScheduler
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
    val pendingCount: Int = 0,
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(
        TaskDatabase.get(application).taskDao(),
        ReminderScheduler(application),
    )
    private val filter = MutableStateFlow(TaskFilter.PENDING)

    val uiState: StateFlow<TasksUiState> =
        combine(repository.tasks, filter) { all, f ->
            TasksUiState(
                tasks = f.apply(all),
                filter = f,
                pendingCount = all.count { !it.isDone },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setFilter(value: TaskFilter) = filter.update { value }

    fun save(task: Task) = viewModelScope.launch { repository.save(task) }

    fun toggleDone(task: Task) = save(task.copy(isDone = !task.isDone))

    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }

    /** Used by "undo": re-inserts the exact same task (same id). */
    fun restore(task: Task) = save(task)

    fun clearDone() = viewModelScope.launch { repository.clearDone() }
}
