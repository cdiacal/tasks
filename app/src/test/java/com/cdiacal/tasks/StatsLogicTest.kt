package com.cdiacal.tasks

import com.cdiacal.tasks.data.TaskCompletion
import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import com.cdiacal.tasks.ui.STATS_WEEKS
import com.cdiacal.tasks.ui.buildStats
import com.cdiacal.tasks.ui.weekStart
import com.cdiacal.tasks.ui.weeklyCounts
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsLogicTest {
    // 2024-05-15 is a Wednesday; its week starts Monday 2024-05-13.
    private val today = LocalDate.of(2024, 5, 15).toEpochDay()
    private val monday = LocalDate.of(2024, 5, 13).toEpochDay()

    @Test fun weekStartIsMonday() {
        assertEquals(monday, weekStart(today))
        assertEquals(monday, weekStart(monday))
        assertEquals(monday, weekStart(monday + 6))
    }

    @Test fun weeklyCountsBucketsByWeekAndIgnoresOutOfRange() {
        val counts = weeklyCounts(
            listOf(today, monday, monday - 1, monday - 7 * 7, monday - 7 * 7 - 1, today + 1),
            today,
        )
        assertEquals(STATS_WEEKS, counts.size)
        assertEquals(2, counts.last()) // today and Monday
        assertEquals(1, counts[STATS_WEEKS - 2]) // Sunday before
        assertEquals(1, counts.first()) // exactly the first day of the window
        assertEquals(4, counts.sum()) // the day before the window and tomorrow are left out
    }

    @Test fun buildStatsCountsCompletionsAndHabits() {
        val habit = Habit(id = 1, name = "h", daysMask = ALL_DAYS, createdAt = LocalDate.of(2024, 5, 13).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        val checks = listOf(HabitCheck(1, monday), HabitCheck(1, monday + 1))
        val completions = listOf(TaskCompletion(1, 1, today), TaskCompletion(2, 1, today - 40))
        val s = buildStats(emptyList(), completions, listOf(habit), checks, today, 0, ZoneOffset.UTC)
        assertEquals(1, s.completedThisWeek)
        assertEquals(1, s.completedLast30)
        assertEquals(2, s.completedTotal)
        // Created Monday: Mon, Tue, Wed scheduled so far, 2 done.
        assertEquals(67, s.habitWeeks.last())
        assertNull(s.habitWeeks.first())
        assertEquals(67, s.habitRate30)
        assertEquals(2, s.habitRows.single().current) // today is still open, so it does not break the run
    }
}
