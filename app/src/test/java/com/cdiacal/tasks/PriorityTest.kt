package com.cdiacal.tasks

import com.cdiacal.tasks.data.BackupJson
import com.cdiacal.tasks.data.Priority
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskSort
import com.cdiacal.tasks.data.priorityKind
import com.cdiacal.tasks.data.sortedFor
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.reminders.computeDigest
import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityTest {
    private fun task(id: Long, priority: Int, done: Boolean = false, due: Long? = null) =
        Task(id = id, title = "t$id", priority = priority, isDone = done, dueDate = due)

    @Test fun levelsMapBothWays() {
        assertEquals(Priority.HIGH, Priority.fromLevel(3))
        assertEquals(Priority.NONE, Priority.fromLevel(99))
        assertEquals(Priority.NONE, Priority.fromLevel(-1))
        assertEquals(Priority.MEDIUM, task(1, 2).priorityKind())
    }

    @Test fun dateOrderIsLeftAsTheDatabaseGaveIt() {
        val tasks = listOf(task(1, 0), task(2, 3), task(3, 1))
        assertEquals(listOf(1L, 2L, 3L), tasks.sortedFor(TaskSort.DATE).map { it.id })
    }

    @Test fun priorityOrderPutsHighFirst() {
        val tasks = listOf(task(1, 0), task(2, 3), task(3, 1), task(4, 2))
        assertEquals(listOf(2L, 4L, 3L, 1L), tasks.sortedFor(TaskSort.PRIORITY).map { it.id })
    }

    @Test fun priorityOrderKeepsDateOrderInsideTheSameLevel() {
        // Already in date order: 5 before 6 before 7. Same priority -> they must stay that way.
        val tasks = listOf(task(5, 2), task(6, 2), task(7, 2), task(8, 3))
        assertEquals(listOf(8L, 5L, 6L, 7L), tasks.sortedFor(TaskSort.PRIORITY).map { it.id })
    }

    @Test fun doneTasksStayAtTheEnd() {
        val tasks = listOf(task(1, 3, done = true), task(2, 0), task(3, 1))
        assertEquals(listOf(3L, 2L, 1L), tasks.sortedFor(TaskSort.PRIORITY).map { it.id })
    }

    @Test fun backupKeepsPriorityAndClampsGarbage() {
        val data = BackupJson.decode(BackupJson.encode(listOf(task(1, 3)), emptyList(), 0))
        assertEquals(3, data.tasks.single().priority)
        val bad = BackupJson.decode("""{"app":"tareas","version":1,"tasks":[{"id":1,"title":"x","priority":42}]}""")
        assertEquals(0, bad.tasks.single().priority)
        val old = BackupJson.decode("""{"app":"tareas","version":1,"tasks":[{"id":1,"title":"x"}]}""")
        assertEquals(0, old.tasks.single().priority)
    }

    @Test fun digestKeepsTheImportantTitlesWhenCapped() {
        val day = 24L * 60 * 60 * 1000
        val today = 20_000L * day
        val tasks = (1..6).map { task(it.toLong(), priority = if (it == 6) 3 else 0, due = today) }
        val digest = computeDigest(tasks, HabitsState(), today, maxTitles = 3)
        assertEquals("t6", digest.titles.first())
        assertEquals(3, digest.moreTitles)
    }
}
