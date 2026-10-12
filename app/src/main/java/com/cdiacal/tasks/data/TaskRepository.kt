package com.cdiacal.tasks.data

import androidx.room.withTransaction
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import com.cdiacal.tasks.reminders.ReminderScheduler
import com.cdiacal.tasks.ui.reminderToUtcDateMillis
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

    val tasks: Flow<List<Task>> = dao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()
    val subtasks: Flow<List<Subtask>> = subtaskDao.observeAll()

    suspend fun save(task: Task) {
        val id = dao.upsert(task)
        // upsert returns the new row id for inserts; for replaces it is the same id.
        reminders.sync(task.copy(id = id))
        onTasksChanged()
    }

    /** Saves the task together with its checklist, replacing any previous steps, atomically. */
    suspend fun saveWithSubtasks(task: Task, steps: List<Subtask>) {
        val id = db.withTransaction {
            val id = dao.upsert(task)
            subtaskDao.deleteForTask(id)
            subtaskDao.insertAll(steps.mapIndexed { index, s -> s.copy(id = 0, taskId = id, position = index) })
            id
        }
        reminders.sync(task.copy(id = id))
        onTasksChanged()
    }

    /** Ticks or unticks one step of a checklist. */
    suspend fun toggleStep(stepId: Long) {
        val step = subtaskDao.getById(stepId) ?: return
        subtaskDao.setDone(stepId, !step.isDone)
        onTasksChanged()
    }

    /**
     * Completes an open task or reopens a done one. A repeating task is rescheduled instead of
     * closed (and its steps start over); the new due date is returned so the UI can say so.
     */
    suspend fun toggleDone(task: Task): Long? {
        if (task.isDone) {
            save(task.copy(isDone = false))
            return null
        }
        val updated = completeTask(task, reminderToUtcDateMillis(System.currentTimeMillis()))
        save(updated)
        if (updated.isDone) return null
        subtaskDao.uncheckAll(task.id)
        return updated.dueDate
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
