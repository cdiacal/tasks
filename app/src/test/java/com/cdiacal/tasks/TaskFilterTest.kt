package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.TaskFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFilterTest {
    private val tasks = listOf(
        Task(id = 1, title = "a", isDone = false),
        Task(id = 2, title = "b", isDone = true),
        Task(id = 3, title = "c", isDone = false),
    )

    @Test fun pendingKeepsOnlyUndone() =
        assertEquals(listOf(1L, 3L), TaskFilter.PENDING.apply(tasks).map { it.id })

    @Test fun doneKeepsOnlyDone() =
        assertEquals(listOf(2L), TaskFilter.DONE.apply(tasks).map { it.id })

    @Test fun allKeepsEverything() =
        assertEquals(3, TaskFilter.ALL.apply(tasks).size)
}
