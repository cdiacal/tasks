package com.cdiacal.tasks

import com.cdiacal.tasks.calendar.CalendarEvent
import com.cdiacal.tasks.calendar.daysCovered
import com.cdiacal.tasks.calendar.groupByDay
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarEventsTest {
    private val zone = ZoneOffset.UTC
    private fun millis(date: LocalDate, hour: Int = 0) = date.atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()
    private val d = LocalDate.of(2024, 5, 15)

    private fun timed(title: String, day: LocalDate, from: Int, to: Int) =
        CalendarEvent(1, title, millis(day, from), millis(day, to), false, "cal")

    private fun allDay(title: String, first: LocalDate, endExclusive: LocalDate) =
        CalendarEvent(2, title, millis(first), millis(endExclusive), true, "cal")

    @Test fun timedEventCoversItsDay() {
        assertEquals(d to d, daysCovered(timed("a", d, 9, 10), zone))
    }

    @Test fun timedEventEndingAtMidnightDoesNotSpillIntoNextDay() {
        val e = CalendarEvent(1, "a", millis(d, 22), millis(d.plusDays(1)), false, "cal")
        assertEquals(d to d, daysCovered(e, zone))
    }

    @Test fun allDayEndIsExclusive() {
        assertEquals(d to d.plusDays(2), daysCovered(allDay("trip", d, d.plusDays(3)), zone))
        assertEquals(d to d, daysCovered(allDay("one", d, d.plusDays(1)), zone))
    }

    @Test fun groupsByDayWithAllDayFirstThenByTime() {
        val events = listOf(timed("late", d, 15, 16), timed("early", d, 8, 9), allDay("holiday", d, d.plusDays(1)))
        val grouped = groupByDay(events, d, 30, zone)
        assertEquals(listOf("holiday", "early", "late"), grouped[d]!!.map { it.title })
    }

    @Test fun multiDayEventAppearsOnEachDayClippedToTheWindow() {
        val trip = allDay("trip", d.minusDays(2), d.plusDays(2)) // started before today
        val grouped = groupByDay(listOf(trip), d, 30, zone)
        assertEquals(listOf(d, d.plusDays(1)), grouped.keys.toList())
    }

    @Test fun eventsBeyondTheWindowAreDropped() {
        val far = timed("far", d.plusDays(40), 9, 10)
        assertEquals(true, groupByDay(listOf(far), d, 30, zone).isEmpty())
    }
}
