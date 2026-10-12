package com.cdiacal.tasks.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val tasks: Flow<List<Task>> = dao.observeAll()

    suspend fun save(task: Task) {
        dao.upsert(task)
    }

    suspend fun delete(task: Task) = dao.delete(task)

    suspend fun clearDone() = dao.deleteDone()
}
