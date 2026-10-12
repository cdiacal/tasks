package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Task

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

data class TaskStats(val pending: Int, val dueToday: Int, val overdue: Int, val done: Int)

enum class DueState { NONE, OVERDUE, SOON }

/** [todayUtcMillis] is today's local date as UTC midnight, the same unit as [Task.dueDate]. */
fun dueState(task: Task, todayUtcMillis: Long): DueState {
    val due = task.dueDate
    return when {
        due == null || task.isDone -> DueState.NONE
        due < todayUtcMillis -> DueState.OVERDUE
        due <= todayUtcMillis + DAY_MILLIS -> DueState.SOON // today or tomorrow
        else -> DueState.NONE
    }
}

fun computeStats(tasks: List<Task>, todayUtcMillis: Long): TaskStats {
    val pending = tasks.filter { !it.isDone }
    return TaskStats(
        pending = pending.size,
        dueToday = pending.count { it.dueDate == todayUtcMillis },
        overdue = pending.count { dueState(it, todayUtcMillis) == DueState.OVERDUE },
        done = tasks.size - pending.size,
    )
}
