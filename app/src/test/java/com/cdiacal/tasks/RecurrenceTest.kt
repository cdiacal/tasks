package com.cdiacal.tasks

import com.cdiacal.tasks.data.Recurrence
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.completeTask
import com.cdiacal.tasks.data.nextDueDate
import com.cdiacal.tasks.data.shiftByDays
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    private val day = 24L * 60 * 60 * 1000
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun utc(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay() * day

    @Test fun dailyOnTimeMovesOneDay() =
        assertEquals(utc(2026, 10, 13), nextDueDate(utc(2026, 10, 12), Recurrence.DAILY, utc(2026, 10, 12)))

    @Test fun weeklyOnTimeMovesOneWeek() =
        assertEquals(utc(2026, 10, 19), nextDueDate(utc(2026, 10, 12), Recurrence.WEEKLY, utc(2026, 10, 12)))

    @Test fun completingEarlyKeepsTheScheduleInsteadOfJumpingAhead() {
        // Due Fri 16th, completed on Mon 12th: next is the following Friday, not 12th + 1 week.
        assertEquals(utc(2026, 10, 23), nextDueDate(utc(2026, 10, 16), Recurrence.WEEKLY, utc(2026, 10, 12)))
    }

    @Test fun overdueTasksSkipToTheFirstFutureOccurrence() {
        // Weekly, due Mon 5th, completed Wed 14th -> next Monday 19th (not an already-past date).
        assertEquals(utc(2026, 10, 19), nextDueDate(utc(2026, 10, 5), Recurrence.WEEKLY, utc(2026, 10, 14)))
        // Daily, due 3 days ago -> tomorrow.
        assertEquals(utc(2026, 10, 13), nextDueDate(utc(2026, 10, 9), Recurrence.DAILY, utc(2026, 10, 12)))
    }

    @Test fun monthlyClampsToTheEndOfShorterMonths() =
        assertEquals(utc(2026, 2, 28), nextDueDate(utc(2026, 1, 31), Recurrence.MONTHLY, utc(2026, 1, 31)))

    @Test fun yearlyHandlesLeapDay() =
        assertEquals(utc(2025, 2, 28), nextDueDate(utc(2024, 2, 29), Recurrence.YEARLY, utc(2024, 2, 29)))

    @Test fun unknownNamesMeanNoRepeat() {
        assertEquals(Recurrence.NONE, Recurrence.fromName("???"))
        assertEquals(Recurrence.NONE, Recurrence.fromName(null))
        assertEquals(Recurrence.WEEKLY, Recurrence.fromName("WEEKLY"))
    }

    @Test fun completingANormalTaskMarksItDone() {
        val done = completeTask(Task(id = 1, title = "x", dueDate = utc(2026, 10, 12)), utc(2026, 10, 12), madrid)
        assertTrue(done.isDone)
        assertEquals(utc(2026, 10, 12), done.dueDate)
    }

    @Test fun completingARepeatingTaskReschedulesAndKeepsItOpen() {
        val task = Task(id = 1, title = "x", dueDate = utc(2026, 10, 12), recurrence = "WEEKLY")
        val next = completeTask(task, utc(2026, 10, 12), madrid)
        assertFalse(next.isDone)
        assertEquals(utc(2026, 10, 19), next.dueDate)
    }

    @Test fun reminderMovesWithTheDueDateAtTheSameLocalTime() {
        val at = LocalDateTime.of(2026, 10, 12, 9, 30).atZone(madrid).toInstant().toEpochMilli()
        val task = Task(id = 1, title = "x", dueDate = utc(2026, 10, 12), reminderAt = at, recurrence = "WEEKLY")
        val next = completeTask(task, utc(2026, 10, 12), madrid)
        val expected = LocalDateTime.of(2026, 10, 19, 9, 30).atZone(madrid).toInstant().toEpochMilli()
        assertEquals(expected, next.reminderAt)
    }

    @Test fun shiftingAcrossTheDstChangeKeepsWallClockTime() {
        // Clocks go back on 2026-10-25 in Madrid: 09:00 stays 09:00 local, though the offset changes.
        val before = LocalDateTime.of(2026, 10, 24, 9, 0).atZone(madrid).toInstant().toEpochMilli()
        val after = LocalDateTime.of(2026, 10, 26, 9, 0).atZone(madrid).toInstant().toEpochMilli()
        assertEquals(after, shiftByDays(before, 2, madrid))
    }

    @Test fun aRepeatingTaskWithoutADueDateJustCompletes() {
        val done = completeTask(Task(id = 1, title = "x", recurrence = "DAILY"), utc(2026, 10, 12), madrid)
        assertTrue(done.isDone)
    }
}
