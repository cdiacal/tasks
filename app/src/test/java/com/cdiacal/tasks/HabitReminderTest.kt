package com.cdiacal.tasks

import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.formatMinutes
import com.cdiacal.tasks.habits.nextReminderMillis
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitReminderTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val weekdays = 0b0011111 // Mon-Fri
    private val nineAm = 9 * 60

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(madrid).toInstant().toEpochMilli()

    private fun local(millis: Long): ZonedDateTime = java.time.Instant.ofEpochMilli(millis).atZone(madrid)

    // 2026-10-12 is a Monday.
    @Test fun laterTodayWhenTheTimeHasNotPassed() {
        val next = nextReminderMillis(weekdays, nineAm, millis(2026, 10, 12, 8, 0), madrid)
        assertEquals(millis(2026, 10, 12, 9, 0), next)
    }

    @Test fun tomorrowWhenTheTimeHasPassed() {
        val next = nextReminderMillis(weekdays, nineAm, millis(2026, 10, 12, 10, 0), madrid)
        assertEquals(millis(2026, 10, 13, 9, 0), next)
    }

    @Test fun exactlyAtTheTimeMeansTheNextDay() {
        val next = nextReminderMillis(ALL_DAYS, nineAm, millis(2026, 10, 12, 9, 0), madrid)
        assertEquals(millis(2026, 10, 13, 9, 0), next)
    }

    @Test fun fridayEveningSkipsTheWeekend() {
        // Fri 2026-10-16 10:00 -> Mon 2026-10-19 09:00
        val next = nextReminderMillis(weekdays, nineAm, millis(2026, 10, 16, 10, 0), madrid)
        assertEquals(millis(2026, 10, 19, 9, 0), next)
    }

    @Test fun weeklyHabitWaitsAWholeWeek() {
        val wednesdayOnly = 0b0000100
        // Wed 2026-10-14 10:00, reminder already passed -> next Wed 2026-10-21 09:00
        val next = nextReminderMillis(wednesdayOnly, nineAm, millis(2026, 10, 14, 10, 0), madrid)
        assertEquals(millis(2026, 10, 21, 9, 0), next)
    }

    @Test fun noScheduledDaysMeansNoReminder() {
        assertNull(nextReminderMillis(0, nineAm, millis(2026, 10, 12, 8, 0), madrid))
    }

    @Test fun keepsLocalTimeAcrossTheDstChange() {
        // Clocks go back on Sunday 2026-10-25 in Madrid; 09:00 must still be 09:00 local.
        val next = nextReminderMillis(ALL_DAYS, nineAm, millis(2026, 10, 24, 10, 0), madrid)!!
        val local = local(next)
        assertEquals(25, local.dayOfMonth)
        assertEquals(9, local.hour)
        assertEquals(0, local.minute)
    }

    @Test fun formatsMinutesAsClockTime() {
        assertEquals("09:00", formatMinutes(540))
        assertEquals("08:05", formatMinutes(485))
        assertEquals("23:59", formatMinutes(23 * 60 + 59))
    }
}
