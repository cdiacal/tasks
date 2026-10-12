package com.cdiacal.tasks.data

import com.cdiacal.tasks.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val reminders: ReminderScheduler,
) {
    val tasks: Flow<List<Task>> = dao.observeAll()

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
}
