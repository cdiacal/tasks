package com.cdiacal.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One step of a task's checklist. */
@Entity(tableName = "subtasks")
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    /** Order inside the task (0 = first). */
    val position: Int = 0,
)
