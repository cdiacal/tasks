package com.cdiacal.tasks

import com.cdiacal.tasks.ui.combineReminder
import com.cdiacal.tasks.ui.reminderHour
import com.cdiacal.tasks.ui.reminderMinute
import com.cdiacal.tasks.ui.reminderToUtcDateMillis
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    // Material date picker returns UTC midnight of the chosen day: 2026-03-15.
    private val march15Utc = ZonedDateTime.of(2026, 3, 15, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    @Test fun combinesUtcDateWithLocalTime() {
        val millis = combineReminder(march15Utc, 9, 30, madrid)
        val local = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), madrid)
        assertEquals(2026, local.year)
        assertEquals(3, local.monthValue)
        assertEquals(15, local.dayOfMonth)
        assertEquals(9, local.hour)
        assertEquals(30, local.minute)
    }

    @Test fun keepsChosenDayInZonesAheadOfUtc() {
        val millis = combineReminder(march15Utc, 0, 15, tokyo)
        val local = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), tokyo)
        assertEquals(15, local.dayOfMonth)
    }

    @Test fun roundTripsDateAndTime() {
        val millis = combineReminder(march15Utc, 18, 45, madrid)
        assertEquals(march15Utc, reminderToUtcDateMillis(millis, madrid))
        assertEquals(18, reminderHour(millis, madrid))
        assertEquals(45, reminderMinute(millis, madrid))
    }
}
