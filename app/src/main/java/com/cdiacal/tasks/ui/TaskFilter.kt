package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Task

enum class TaskFilter {
    PENDING,
    DONE,
    ALL,
    ;

    fun apply(tasks: List<Task>): List<Task> = when (this) {
        PENDING -> tasks.filter { !it.isDone }
        DONE -> tasks.filter { it.isDone }
        ALL -> tasks
    }
}
