package com.cdiacal.tasks.data

import androidx.room.withTransaction
import com.cdiacal.tasks.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val db: TaskDatabase,
    private val reminders: ReminderScheduler,
    /** Called after any change to tasks (used to refresh the home-screen widget). */
    private val onTasksChanged: suspend () -> Unit = {},
) {
    private val dao = db.taskDao()
    private val categoryDao = db.categoryDao()

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
        return BackupJson.encode(tasks, categoryDao.getAll(), System.currentTimeMillis()) to tasks.size
    }

    /** Replaces ALL current tasks and categories with the backup's, then rebuilds reminders. */
    suspend fun restoreBackup(data: BackupData) {
        val previous = dao.getAll()
        db.withTransaction {
            dao.deleteAll()
            categoryDao.deleteAll()
            categoryDao.insertAll(data.categories)
            dao.insertAll(data.tasks)
        }
        previous.forEach { reminders.cancel(it.id) }
        data.tasks.forEach(reminders::sync)
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
