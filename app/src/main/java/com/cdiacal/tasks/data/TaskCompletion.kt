package com.cdiacal.tasks.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One row per time a task was completed (local epoch day). It is a log kept apart from the tasks
 * so that repeating tasks, deleted tasks and "clear done" do not erase the history.
 */
@Entity(tableName = "task_completions")
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val epochDay: Long,
)

@Dao
interface TaskCompletionDao {
    @Query("SELECT * FROM task_completions")
    fun observeAll(): Flow<List<TaskCompletion>>

    @Insert
    suspend fun insert(completion: TaskCompletion)

    /** Reopening a task takes back its most recent completion. */
    @Query(
        "DELETE FROM task_completions WHERE id = " +
            "(SELECT id FROM task_completions WHERE taskId = :taskId ORDER BY id DESC LIMIT 1)",
    )
    suspend fun deleteLatestFor(taskId: Long)

    // Used by backup/restore.
    @Query("SELECT * FROM task_completions")
    suspend fun getAll(): List<TaskCompletion>

    @Query("DELETE FROM task_completions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<TaskCompletion>)
}
