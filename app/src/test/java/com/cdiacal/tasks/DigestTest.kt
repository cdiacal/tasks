package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.reminders.computeDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestTest {
    private val day = 24L * 60 * 60 * 1000
    private val today = 20_000L * day
    private fun task(title: String, due: Long?, done: Boolean = false) = Task(title = title, dueDate = due, isDone = done)
    private val noHabits = HabitsState()

    @Test fun countsTodayAndOverdueSeparately() {
        val digest = computeDigest(
            listOf(task("a", today), task("b", today - day), task("c", today - 3 * day), task("d", today + day)),
            noHabits,
            today,
        )
        assertEquals(1, digest.dueToday)
        assertEquals(2, digest.overdue)
    }

    @Test fun ignoresDoneTasksAndUndatedOnes() {
        val digest = computeDigest(listOf(task("a", today, done = true), task("b", null)), noHabits, today)
        assertTrue(digest.isEmpty)
    }

    @Test fun overdueTitlesComeFirstOldestFirst() {
        val digest = computeDigest(
            listOf(task("hoy", today), task("ayer", today - day), task("hace una semana", today - 7 * day)),
            noHabits,
            today,
        )
        assertEquals(listOf("hace una semana", "ayer", "hoy"), digest.titles)
    }

    @Test fun capsTheTitlesAndCountsTheRest() {
        val tasks = (1..8).map { task("t$it", today) }
        val digest = computeDigest(tasks, noHabits, today, maxTitles = 5)
        assertEquals(5, digest.titles.size)
        assertEquals(3, digest.moreTitles)
    }

    @Test fun pendingHabitsCountAsContent() {
        val digest = computeDigest(emptyList(), HabitsState(todayDone = 1, todayTotal = 3), today)
        assertEquals(2, digest.habitsPending)
        assertFalse(digest.isEmpty)
    }

    @Test fun nothingToSayMeansEmpty() = assertTrue(computeDigest(emptyList(), noHabits, today).isEmpty)
}
