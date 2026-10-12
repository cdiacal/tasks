package com.cdiacal.tasks

import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.buildMonthGrid
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthGridTest {
    private val weekdays = 0b0011111 // Mon-Fri
    private val oct2026 = YearMonth.of(2026, 10)

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    // 2026-10-01 is a Thursday, so three blank cells (Mon, Tue, Wed) come first.
    @Test fun layoutStartsOnTheRightWeekday() {
        val grid = buildMonthGrid(oct2026, ALL_DAYS, emptySet(), day(2026, 10, 1), day(2026, 10, 12))
        assertEquals(3, grid.leadingBlanks)
        assertEquals(31, grid.days.size)
    }

    @Test fun februaryStartingOnSunday() {
        val grid = buildMonthGrid(YearMonth.of(2026, 2), ALL_DAYS, emptySet(), day(2026, 1, 1), day(2026, 2, 10))
        assertEquals(6, grid.leadingBlanks)
        assertEquals(28, grid.days.size)
    }

    @Test fun marksScheduledDoneTodayAndFuture() {
        val done = setOf(day(2026, 10, 1), day(2026, 10, 2), day(2026, 10, 5))
        val grid = buildMonthGrid(oct2026, weekdays, done, day(2026, 10, 1), day(2026, 10, 12))
        val byDay = grid.days.associateBy { it.dayOfMonth }
        assertTrue(byDay.getValue(1).done)
        assertTrue(byDay.getValue(5).scheduled)
        assertFalse(byDay.getValue(3).scheduled) // Saturday
        assertTrue(byDay.getValue(12).isToday)
        assertFalse(byDay.getValue(12).isFuture)
        assertTrue(byDay.getValue(13).isFuture)
    }

    @Test fun percentCountsScheduledDaysUpToToday() {
        // Scheduled weekdays Oct 1..12: 1,2,5,6,7,8,9,12 = 8; done 3 of them.
        val done = setOf(day(2026, 10, 1), day(2026, 10, 2), day(2026, 10, 5))
        val grid = buildMonthGrid(oct2026, weekdays, done, day(2026, 10, 1), day(2026, 10, 12))
        assertEquals(8, grid.scheduledSoFar)
        assertEquals(3, grid.completed)
        assertEquals(38, grid.percent)
    }

    @Test fun daysBeforeTheHabitExistedDoNotCount() {
        // Created Oct 8: scheduled so far = Oct 8, 9, 12.
        val grid = buildMonthGrid(oct2026, weekdays, emptySet(), day(2026, 10, 8), day(2026, 10, 12))
        assertEquals(3, grid.scheduledSoFar)
        assertEquals(0, grid.percent)
    }

    @Test fun aMonthBeforeCreationHasNoPercent() {
        val grid = buildMonthGrid(YearMonth.of(2026, 9), ALL_DAYS, emptySet(), day(2026, 10, 8), day(2026, 10, 12))
        assertEquals(0, grid.scheduledSoFar)
        assertEquals(0, grid.percent)
    }

    @Test fun anEarlierTickExtendsTheHistoryBackwards() {
        val done = setOf(day(2026, 9, 30))
        val grid = buildMonthGrid(YearMonth.of(2026, 9), ALL_DAYS, done, day(2026, 10, 8), day(2026, 10, 12))
        assertEquals(1, grid.scheduledSoFar) // only Sep 30 is in history
        assertEquals(1, grid.completed)
    }
}
