package com.cdiacal.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isDone: Boolean = false,
    /** Due date as epoch millis at UTC midnight, or null when there is none. */
    val dueDate: Long? = null,
    /** Reminder moment as epoch millis (absolute instant), or null when there is none. */
    val reminderAt: Long? = null,
    /** Id of the [Category] this task belongs to, or null for none. */
    val categoryId: Long? = null,
    /** A [Recurrence] name. Repeating tasks always have a due date (their anchor). */
    val recurrence: String = Recurrence.NONE.name,
    /** A [Priority] level: 0 none, 1 low, 2 medium, 3 high. */
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
