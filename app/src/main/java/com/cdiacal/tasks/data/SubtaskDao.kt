package com.cdiacal.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    suspend fun forTask(taskId: Long): List<Subtask>

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getById(id: Long): Subtask?

    @Query("UPDATE subtasks SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    /** A repeating task starts its steps over each time it comes round again. */
    @Query("UPDATE subtasks SET isDone = 0 WHERE taskId = :taskId")
    suspend fun uncheckAll(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtasks: List<Subtask>)

    // Used by backup/restore.
    @Query("SELECT * FROM subtasks")
    suspend fun getAll(): List<Subtask>

    @Query("DELETE FROM subtasks")
    suspend fun deleteAll()
}
