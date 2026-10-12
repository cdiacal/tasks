package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Task

/** Keeps every task when [categoryId] is null, otherwise only that category's tasks. */
fun List<Task>.inCategory(categoryId: Long?): List<Task> =
    if (categoryId == null) this else filter { it.categoryId == categoryId }

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
