package com.cdiacal.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Pending first, then by nearest due date (no date last), then newest.
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY isDone ASC,
                 CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC,
                 dueDate ASC,
                 createdAt DESC
        """,
    )
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    // Live versions of the two widget queries above: the widget keeps collecting these so it
    // redraws on its own whenever a task changes, instead of relying on a manual refresh.
    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 0")
    fun observePendingCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM tasks WHERE isDone = 0
        ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, dueDate ASC, createdAt DESC
        LIMIT :limit
        """,
    )
    fun observePending(limit: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 0")
    suspend fun pendingCount(): Int

    // Same ordering as the app: nearest due date first, undated last, then newest.
    @Query(
        """
        SELECT * FROM tasks WHERE isDone = 0
        ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, dueDate ASC, createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun pending(limit: Int): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Query("SELECT * FROM tasks WHERE isDone = 0 AND reminderAt IS NOT NULL AND reminderAt > :now")
    suspend fun pendingReminders(now: Long): List<Task>

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteDone()
}
