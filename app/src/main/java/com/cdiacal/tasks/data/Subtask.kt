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

/** True when there is at least one step and every step is ticked. */
fun List<Subtask>.allDone(): Boolean = isNotEmpty() && all { it.isDone }

/** Ticking the last open step completes the task it belongs to (unless it is already done). */
fun shouldCompleteTask(task: Task, steps: List<Subtask>): Boolean = !task.isDone && steps.allDone()
