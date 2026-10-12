package com.cdiacal.tasks

import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import com.cdiacal.tasks.habits.buildHabitsState
import com.cdiacal.tasks.habits.computeStreaks
import com.cdiacal.tasks.habits.isScheduled
import com.cdiacal.tasks.habits.weekProgress
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {
    // 2026-10-12 is a Monday.
    private val monday = LocalDate.of(2026, 10, 12).toEpochDay()
    private val weekdays = 0b0011111 // Mon-Fri

    @Test fun mondayIsIndexZero() {
        assertTrue(isScheduled(0b0000001, monday))
        assertFalse(isScheduled(0b0000001, monday + 1))
    }

    @Test fun dailyStreakCountsConsecutiveDaysIncludingToday() {
        val done = (0L..4L).map { monday - it }.toSet()
        val s = computeStreaks(ALL_DAYS, done, startDay = monday - 10, today = monday)
        assertEquals(5, s.current)
        assertEquals(5, s.best)
    }

    @Test fun openTodayDoesNotBreakTheStreak() {
        val done = setOf(monday - 1, monday - 2, monday - 3)
        val s = computeStreaks(ALL_DAYS, done, startDay = monday - 10, today = monday)
        assertEquals(3, s.current)
    }

    @Test fun missedDayResetsCurrentButKeepsBest() {
        // done: -6,-5,-4 (3 in a row), missed -3, done -2,-1,0 (3 in a row) plus -7 earlier
        val done = setOf(-7L, -6L, -5L, -4L, -2L, -1L, 0L).map { monday + it }.toSet()
        val s = computeStreaks(ALL_DAYS, done, startDay = monday - 10, today = monday)
        assertEquals(3, s.current)
        assertEquals(4, s.best)
    }

    @Test fun missedYesterdayBreaksCurrent() {
        val done = setOf(monday - 3, monday - 2)
        val s = computeStreaks(ALL_DAYS, done, startDay = monday - 10, today = monday)
        assertEquals(0, s.current)
        assertEquals(2, s.best)
    }

    @Test fun unscheduledWeekendDoesNotBreakAWeekdayHabit() {
        // Thu 8, Fri 9, (Sat/Sun not scheduled), Mon 12 = today.
        val done = setOf(monday - 4, monday - 3, monday)
        val s = computeStreaks(weekdays, done, startDay = monday - 20, today = monday)
        assertEquals(3, s.current)
    }

    @Test fun ticksOnUnscheduledDaysAreIgnored() {
        val done = setOf(monday - 2) // a Saturday
        val s = computeStreaks(weekdays, done, startDay = monday - 20, today = monday)
        assertEquals(0, s.best)
    }

    @Test fun weekProgressCountsScheduledDaysOnly() {
        // Last 7 days ending Monday: Tue..Sun + Mon. Weekdays scheduled: Tue,Wed,Thu,Fri,Mon = 5.
        val done = setOf(monday - 6, monday - 5, monday)
        assertEquals(3 to 5, weekProgress(weekdays, done, monday - 30, monday))
    }

    @Test fun weekProgressIgnoresDaysBeforeCreation() {
        // Habit created today: only today counts.
        assertEquals(0 to 1, weekProgress(ALL_DAYS, emptySet(), startDay = monday, today = monday))
    }

    @Test fun stateAggregatesTodayAndWeek() {
        val zone = ZoneId.of("UTC")
        val created = monday * 86_400_000L - 10 * 86_400_000L
        val habits = listOf(
            Habit(id = 1, name = "Leer", daysMask = ALL_DAYS, createdAt = created),
            Habit(id = 2, name = "Gym", daysMask = 0b0000010, createdAt = created), // Tuesdays only
        )
        val checks = listOf(HabitCheck(1, monday), HabitCheck(1, monday - 1))
        val state = buildHabitsState(habits, checks, monday, zone)
        assertEquals(1, state.todayTotal) // "Gym" is not scheduled on Monday
        assertEquals(1, state.todayDone)
        assertEquals(2, state.bestCurrentStreak)
        assertEquals(7, state.items[0].week.size)
        assertTrue(state.items[0].week.last().isToday)
    }
}
