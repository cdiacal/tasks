package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Priority
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.priorityKind

/** Section of the list when it is ordered by priority. */
enum class GroupKey { HIGH, MEDIUM, LOW, NONE, DONE }

data class TaskGroup(val key: GroupKey, val tasks: List<Task>)

fun groupKeyOf(task: Task): GroupKey = if (task.isDone) {
    GroupKey.DONE
} else {
    when (task.priorityKind()) {
        Priority.HIGH -> GroupKey.HIGH
        Priority.MEDIUM -> GroupKey.MEDIUM
        Priority.LOW -> GroupKey.LOW
        Priority.NONE -> GroupKey.NONE
    }
}

/**
 * Splits an already priority-ordered list into its sections (high, medium, low, none, done),
 * dropping empty ones. Order inside each section is preserved.
 */
fun priorityGroups(tasks: List<Task>): List<TaskGroup> {
    val byKey = tasks.groupBy(::groupKeyOf)
    return GroupKey.entries.mapNotNull { key -> byKey[key]?.let { TaskGroup(key, it) } }
}
