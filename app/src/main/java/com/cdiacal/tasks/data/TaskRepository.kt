package com.cdiacal.tasks.data

import androidx.room.withTransaction
import com.cdiacal.tasks.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val db: TaskDatabase,
    private val reminders: ReminderScheduler,
) {
    private val dao = db.taskDao()
    private val categoryDao = db.categoryDao()

    val tasks: Flow<List<Task>> = dao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()

    suspend fun save(task: Task) {
        val id = dao.upsert(task)
        // upsert returns the new row id for inserts; for replaces it is the same id.
        reminders.sync(task.copy(id = id))
    }

    suspend fun delete(task: Task) {
        dao.delete(task)
        reminders.cancel(task.id)
    }

    // Done tasks already had their alarm cancelled when they were marked done.
    suspend fun clearDone() = dao.deleteDone()

    suspend fun saveCategory(category: Category) {
        categoryDao.upsert(category)
    }

    /** Deleting a category keeps its tasks; they just become uncategorized. */
    suspend fun deleteCategory(category: Category) = db.withTransaction {
        dao.clearCategory(category.id)
        categoryDao.delete(category)
    }
}
