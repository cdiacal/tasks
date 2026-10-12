package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskSort
import com.cdiacal.tasks.data.sortedFor
import com.cdiacal.tasks.ui.GroupKey
import com.cdiacal.tasks.ui.groupKeyOf
import com.cdiacal.tasks.ui.priorityGroups
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskGroupsTest {
    private fun task(id: Long, priority: Int, done: Boolean = false) = Task(id = id, title = "t$id", priority = priority, isDone = done)

    @Test fun doneTasksAlwaysGoToTheirOwnGroup() =
        assertEquals(GroupKey.DONE, groupKeyOf(task(1, 3, done = true)))

    @Test fun eachPriorityMapsToItsGroup() {
        assertEquals(GroupKey.HIGH, groupKeyOf(task(1, 3)))
        assertEquals(GroupKey.MEDIUM, groupKeyOf(task(1, 2)))
        assertEquals(GroupKey.LOW, groupKeyOf(task(1, 1)))
        assertEquals(GroupKey.NONE, groupKeyOf(task(1, 0)))
    }

    @Test fun groupsComeInPriorityOrderAndSkipEmptyOnes() {
        val tasks = listOf(task(1, 0), task(2, 3), task(3, 3), task(4, 1, done = true)).sortedFor(TaskSort.PRIORITY)
        val groups = priorityGroups(tasks)
        assertEquals(listOf(GroupKey.HIGH, GroupKey.NONE, GroupKey.DONE), groups.map { it.key })
        assertEquals(listOf(2L, 3L), groups.first().tasks.map { it.id })
    }

    @Test fun anEmptyListHasNoGroups() = assertEquals(emptyList<Any>(), priorityGroups(emptyList()))
}
