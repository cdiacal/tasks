package com.cdiacal.tasks

import com.cdiacal.tasks.reminders.Snooze
import com.cdiacal.tasks.reminders.snoozeUntil
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private fun millis(y: Int, m: Int, d: Int, h: Int, mi: Int = 0) =
        LocalDateTime.of(y, m, d, h, mi).atZone(madrid).toInstant().toEpochMilli()

    @Test fun anHourIsExactlyAnHourLater() {
        val now = millis(2026, 10, 12, 15, 20)
        assertEquals(now + 3_600_000, snoozeUntil(Snooze.HOUR, now, madrid))
    }

    @Test fun tomorrowIsNineAmNextDay() =
        assertEquals(millis(2026, 10, 13, 9), snoozeUntil(Snooze.TOMORROW, millis(2026, 10, 12, 15, 20), madrid))

    @Test fun tomorrowFromEarlyMorningIsStillTheNextDay() =
        assertEquals(millis(2026, 10, 13, 9), snoozeUntil(Snooze.TOMORROW, millis(2026, 10, 12, 7, 0), madrid))

    @Test fun tomorrowFromLateAtNightCrossesTheMonth() =
        assertEquals(millis(2026, 11, 1, 9), snoozeUntil(Snooze.TOMORROW, millis(2026, 10, 31, 23, 45), madrid))
}
