package com.cdiacal.tasks.data

import androidx.room.withTransaction
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import com.cdiacal.tasks.reminders.ReminderScheduler
import com.cdiacal.tasks.ui.reminderToUtcDateMillis
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val db: TaskDatabase,
    private val reminders: ReminderScheduler,
    private val habitReminders: HabitReminderScheduler,
    /** Called after any change to tasks (used to refresh the home-screen widget). */
    private val onTasksChanged: suspend () -> Unit = {},
) {
    private val dao = db.taskDao()
    private val categoryDao = db.categoryDao()
    private val habitDao = db.habitDao()
    private val subtaskDao = db.subtaskDao()
    private val completionDao = db.completionDao()

    val tasks: Flow<List<Task>> = dao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()
    val subtasks: Flow<List<Subtask>> = subtaskDao.observeAll()
    val completions: Flow<List<TaskCompletion>> = completionDao.observeAll()

    suspend fun save(task: Task) {
        val id = dao.upsert(task)
        // upsert returns the new row id for inserts; for replaces it is the same id.
        reminders.sync(task.copy(id = id))
        onTasksChanged()
    }

    /** Saves the task together with its checklist, replacing any previous steps, atomically. */
    suspend fun saveWithSubtasks(task: Task, steps: List<Subtask>, completeIfAllStepsDone: Boolean = false): Long? {
        val id = db.withTransaction {
            val id = dao.upsert(task)
            subtaskDao.deleteForTask(id)
            subtaskDao.insertAll(steps.mapIndexed { index, s -> s.copy(id = 0, taskId = id, position = index) })
            id
        }
        reminders.sync(task.copy(id = id))
        onTasksChanged()
        // The last step was ticked in the editor: complete the task like the checkbox would.
        return if (completeIfAllStepsDone && shouldCompleteTask(task, steps)) toggleDone(task.copy(id = id)) else null
    }

    /**
     * Ticks or unticks one step. Ticking the last open step completes the task (a repeating task is
     * rescheduled and its steps start over); the new due date is returned in that case.
     */
    suspend fun toggleStep(stepId: Long): Long? {
        val step = subtaskDao.getById(stepId) ?: return null
        subtaskDao.setDone(stepId, !step.isDone)
        val task = dao.getById(step.taskId)
        val completed = if (task != null && shouldCompleteTask(task, subtaskDao.forTask(step.taskId))) {
            toggleDone(task)
        } else {
            null
        }
        onTasksChanged()
        return completed
    }

    /**
     * Completes an open task or reopens a done one. A repeating task is rescheduled instead of
     * closed (and its steps start over); the new due date is returned so the UI can say so.
     */
    suspend fun toggleDone(task: Task): Long? {
        if (task.isDone) {
            save(task.copy(isDone = false))
            completionDao.deleteLatestFor(task.id)
            return null
        }
        val updated = completeTask(task, reminderToUtcDateMillis(System.currentTimeMillis()))
        save(updated)
        completionDao.insert(TaskCompletion(taskId = task.id, epochDay = LocalDate.now().toEpochDay()))
        if (updated.isDone) return null
        subtaskDao.uncheckAll(task.id)
        return updated.dueDate
    }

    /**
     * Takes back a completion: puts [original] (the task as it was before) and its steps back and
     * drops the completion from the log. Works for repeating tasks, which had moved to a new date.
     */
    suspend fun undoCompletion(original: Task, steps: List<Subtask>) {
        db.withTransaction {
            dao.upsert(original)
            subtaskDao.deleteForTask(original.id)
            subtaskDao.insertAll(steps.map { it.copy(id = 0, taskId = original.id) })
            completionDao.deleteLatestFor(original.id)
        }
        reminders.sync(original)
        onTasksChanged()
    }

    suspend fun delete(task: Task) {
        db.withTransaction {
            subtaskDao.deleteForTask(task.id)
            dao.delete(task)
        }
        reminders.cancel(task.id)
        onTasksChanged()
    }

    // Done tasks already had their alarm cancelled when they were marked done.
    suspend fun clearDone() {
        dao.deleteDone()
        onTasksChanged()
    }

    /** Serializes everything to the backup JSON; returns the text and how many tasks it holds. */
    suspend fun exportBackup(): Pair<String, Int> {
        val tasks = dao.getAll()
        val json = BackupJson.encode(
            tasks = tasks,
            categories = categoryDao.getAll(),
            exportedAt = System.currentTimeMillis(),
            habits = habitDao.getAllHabits(),
            habitChecks = habitDao.getAllChecks(),
            subtasks = subtaskDao.getAll(),
            completions = completionDao.getAll(),
        )
        return json to tasks.size
    }

    /** Replaces ALL current tasks and categories with the backup's, then rebuilds reminders. */
    suspend fun restoreBackup(data: BackupData) {
        val previous = dao.getAll()
        val previousHabits = habitDao.getAllHabits()
        db.withTransaction {
            dao.deleteAll()
            categoryDao.deleteAll()
            categoryDao.insertAll(data.categories)
            dao.insertAll(data.tasks)
            subtaskDao.deleteAll()
            subtaskDao.insertAll(data.subtasks)
            // Old backups have no completion log: keep the current one in that case.
            data.completions?.let { completionDao.deleteAll(); completionDao.insertAll(it) }
            // Old backups have no habits section: keep the current habits in that case.
            data.habits?.let { habits ->
                habitDao.deleteAllChecks()
                habitDao.deleteAllHabits()
                habitDao.insertHabits(habits)
                habitDao.insertChecks(data.habitChecks)
            }
        }
        previous.forEach { reminders.cancel(it.id) }
        data.tasks.forEach(reminders::sync)
        if (data.habits != null) {
            previousHabits.forEach { habitReminders.cancel(it.id) }
            data.habits.forEach(habitReminders::sync)
        }
        onTasksChanged()
    }

    suspend fun saveCategory(category: Category) {
        categoryDao.upsert(category)
    }

    /** Deleting a category keeps its tasks; they just become uncategorized. */
    suspend fun deleteCategory(category: Category) = db.withTransaction {
        dao.clearCategory(category.id)
        categoryDao.delete(category)
    }
}
