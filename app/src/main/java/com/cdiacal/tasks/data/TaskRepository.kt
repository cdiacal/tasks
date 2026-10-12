package com.cdiacal.tasks.data

import androidx.room.withTransaction
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import com.cdiacal.tasks.reminders.ReminderScheduler
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

    val tasks: Flow<List<Task>> = dao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()

    suspend fun save(task: Task) {
        val id = dao.upsert(task)
        // upsert returns the new row id for inserts; for replaces it is the same id.
        reminders.sync(task.copy(id = id))
        onTasksChanged()
    }

    suspend fun delete(task: Task) {
        dao.delete(task)
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
