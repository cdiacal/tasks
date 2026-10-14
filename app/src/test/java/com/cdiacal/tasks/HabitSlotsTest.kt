package com.cdiacal.tasks

import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.DaySlot
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import com.cdiacal.tasks.habits.buildHabitsState
import com.cdiacal.tasks.habits.completeDays
import com.cdiacal.tasks.habits.encodeSlotReminders
import com.cdiacal.tasks.habits.isDayComplete
import com.cdiacal.tasks.habits.isSlotDone
import com.cdiacal.tasks.habits.parseSlotReminders
import com.cdiacal.tasks.habits.reminderTimes
import com.cdiacal.tasks.habits.nextPendingSlot
import com.cdiacal.tasks.habits.slotsOf
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitSlotsTest {
    private val morningNight = DaySlot.MORNING.bit or DaySlot.NIGHT.bit
    private val today = LocalDate.of(2024, 5, 15).toEpochDay()

    @Test fun slotsComeInDayOrder() {
        assertEquals(listOf(DaySlot.MORNING, DaySlot.NIGHT), slotsOf(morningNight))
        assertEquals(emptyList<DaySlot>(), slotsOf(0))
    }

    @Test fun plainHabitIsDoneByAnyTick() {
        assertFalse(isDayComplete(0, emptySet()))
        assertTrue(isDayComplete(0, setOf(0)))
        assertTrue(isDayComplete(0, setOf(2))) // slots were removed later: the old tick still counts
    }

    @Test fun slottedHabitNeedsEverySlot() {
        assertFalse(isDayComplete(morningNight, setOf(DaySlot.MORNING.value)))
        assertTrue(isDayComplete(morningNight, setOf(DaySlot.MORNING.value, DaySlot.NIGHT.value)))
    }

    @Test fun aWholeDayTickFromBeforeSlotsCoversAllSlots() {
        assertTrue(isDayComplete(morningNight, setOf(0)))
    }

    @Test fun completeDaysIgnoresPartialDays() {
        val checks = listOf(
            HabitCheck(1, 10, DaySlot.MORNING.value),
            HabitCheck(1, 11, DaySlot.MORNING.value),
            HabitCheck(1, 11, DaySlot.NIGHT.value),
            HabitCheck(1, 12, 0),
        )
        assertEquals(setOf(11L, 12L), completeDays(morningNight, checks))
    }

    @Test fun nextPendingSlotPicksTheFirstOpenOne() {
        assertEquals(DaySlot.MORNING.value, nextPendingSlot(morningNight, emptySet()))
        assertEquals(DaySlot.NIGHT.value, nextPendingSlot(morningNight, setOf(DaySlot.MORNING.value)))
        assertNull(nextPendingSlot(morningNight, setOf(DaySlot.MORNING.value, DaySlot.NIGHT.value)))
        assertEquals(0, nextPendingSlot(0, emptySet()))
        assertNull(nextPendingSlot(0, setOf(0)))
    }

    @Test fun stateCountsSlotsAndKeepsStreaksByCompleteDays() {
        val created = LocalDate.of(2024, 5, 13).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val habit = Habit(id = 1, name = "Pastilla", daysMask = ALL_DAYS, createdAt = created, slotsMask = morningNight)
        val checks = listOf(
            HabitCheck(1, today - 1, DaySlot.MORNING.value),
            HabitCheck(1, today - 1, DaySlot.NIGHT.value),
            HabitCheck(1, today, DaySlot.MORNING.value),
        )
        val state = buildHabitsState(listOf(habit), checks, today, ZoneOffset.UTC)
        val item = state.items.single()
        assertEquals(1, state.todaySlotsDone)
        assertEquals(2, state.todaySlotsTotal)
        assertFalse(item.doneToday) // the night one is still open
        assertEquals(listOf(true, false), item.slots.map { it.done })
        assertEquals(1, item.streaks.current) // yesterday was complete, today still open
        assertTrue(item.week.last().partial)
    }

    @Test fun slotRemindersRoundTripAndSkipGarbage() {
        val times = mapOf(DaySlot.MORNING.value to 480, DaySlot.NIGHT.value to 1260)
        assertEquals("1=480;3=1260", encodeSlotReminders(times))
        assertEquals(times, parseSlotReminders("1=480;3=1260"))
        assertEquals(mapOf(2 to 840), parseSlotReminders("x;9=100;2=840;3=99999;1=a"))
        assertEquals(emptyMap<Int, Int>(), parseSlotReminders(""))
    }

    @Test fun plainHabitHasOneReminderWithSlotZero() {
        val habit = Habit(id = 1, name = "h", reminderMinutes = 540)
        assertEquals(listOf(0 to 540), reminderTimes(habit))
        assertEquals(emptyList<Pair<Int, Int>>(), reminderTimes(Habit(id = 2, name = "h")))
    }

    @Test fun slottedHabitHasOneReminderPerChosenSlotWithATime() {
        val habit = Habit(
            id = 1, name = "h", reminderMinutes = 540, slotsMask = morningNight,
            slotReminders = "1=480;2=840;3=1260",
        )
        // The single reminder is ignored, and the noon time is not used: noon is not a chosen slot.
        assertEquals(listOf(1 to 480, 3 to 1260), reminderTimes(habit))
        assertEquals(listOf(3 to 1260), reminderTimes(habit.copy(slotReminders = "3=1260")))
    }

    @Test fun wholeDayTickCoversEverySlot() {
        assertTrue(isSlotDone(2, setOf(0)))
        assertFalse(isSlotDone(2, setOf(1)))
        assertTrue(isSlotDone(2, setOf(2)))
    }
}
