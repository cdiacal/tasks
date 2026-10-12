package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.DueState
import com.cdiacal.tasks.ui.computeStats
import com.cdiacal.tasks.ui.dueState
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskStatsTest {
    private val day = 24L * 60 * 60 * 1000
    private val today = 20_000L * day

    private fun task(due: Long?, done: Boolean = false) = Task(title = "t", dueDate = due, isDone = done)

    @Test fun dueStateClassifiesByDay() {
        assertEquals(DueState.OVERDUE, dueState(task(today - day), today))
        assertEquals(DueState.SOON, dueState(task(today), today))
        assertEquals(DueState.SOON, dueState(task(today + day), today))
        assertEquals(DueState.NONE, dueState(task(today + 2 * day), today))
        assertEquals(DueState.NONE, dueState(task(null), today))
    }

    @Test fun doneTasksAreNeverOverdue() =
        assertEquals(DueState.NONE, dueState(task(today - day, done = true), today))

    @Test fun statsCountPendingTodayOverdueAndDone() {
        val stats = computeStats(
            listOf(task(today), task(today - day), task(null), task(today, done = true)),
            today,
        )
        assertEquals(3, stats.pending)
        assertEquals(1, stats.dueToday)
        assertEquals(1, stats.overdue)
        assertEquals(1, stats.done)
    }
}
