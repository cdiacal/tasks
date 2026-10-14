package com.cdiacal.tasks.habits

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 0 = Monday .. 6 = Sunday. */
fun dayIndex(epochDay: Long): Int = LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1

fun isScheduled(mask: Int, epochDay: Long): Boolean = mask and (1 shl dayIndex(epochDay)) != 0

data class Streaks(val current: Int, val best: Int)

/** Ticks made before the habit's creation date still count, so history starts at the earliest of both. */
private fun firstDay(startDay: Long, done: Set<Long>): Long = minOf(startDay, done.minOrNull() ?: startDay)

/**
 * Streaks count consecutive *scheduled* days: unscheduled days neither extend nor break a streak.
 * A scheduled today that is still open does not break the current streak yet.
 */
fun computeStreaks(mask: Int, done: Set<Long>, startDay: Long, today: Long): Streaks {
    var run = 0
    var best = 0
    for (day in firstDay(startDay, done)..today) {
        if (!isScheduled(mask, day)) continue
        when {
            day in done -> {
                run++
                if (run > best) best = run
            }
            day == today -> Unit
            else -> run = 0
        }
    }
    return Streaks(current = run, best = best)
}

/** (completed, scheduled) over the last 7 days including today. */
fun weekProgress(mask: Int, done: Set<Long>, startDay: Long, today: Long): Pair<Int, Int> {
    val from = firstDay(startDay, done)
    var completed = 0
    var scheduled = 0
    for (day in (today - 6)..today) {
        if (day < from || !isScheduled(mask, day)) continue
        scheduled++
        if (day in done) completed++
    }
    return completed to scheduled
}

data class MonthDay(
    val epochDay: Long,
    val dayOfMonth: Int,
    val scheduled: Boolean,
    val done: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/**
 * One calendar month of a habit's history. [leadingBlanks] empty cells precede day 1 (weeks
 * start on Monday). [completed] / [scheduledSoFar] count only scheduled days from the habit's
 * first day up to today, so the percentage never penalizes days before it existed or still ahead.
 */
data class MonthGrid(
    val month: YearMonth,
    val leadingBlanks: Int,
    val days: List<MonthDay>,
    val completed: Int,
    val scheduledSoFar: Int,
) {
    val percent: Int get() = if (scheduledSoFar == 0) 0 else Math.round(100f * completed / scheduledSoFar)
}

fun buildMonthGrid(month: YearMonth, mask: Int, done: Set<Long>, startDay: Long, today: Long): MonthGrid {
    val from = firstDay(startDay, done)
    var completed = 0
    var scheduledSoFar = 0
    val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
        val day = month.atDay(dayOfMonth).toEpochDay()
        val scheduled = isScheduled(mask, day)
        val isDone = day in done
        if (scheduled && day in from..today) {
            scheduledSoFar++
            if (isDone) completed++
        }
        MonthDay(day, dayOfMonth, scheduled, isDone, isToday = day == today, isFuture = day > today)
    }
    return MonthGrid(month, month.atDay(1).dayOfWeek.value - 1, days, completed, scheduledSoFar)
}

/** The earliest day with any history for the habit: its creation day or its earliest tick. */
fun historyStart(startDay: Long, done: Set<Long>): Long = firstDay(startDay, done)

/** [partial]: some but not all of the day's slots are done. */
data class DayCell(val epochDay: Long, val scheduled: Boolean, val done: Boolean, val isToday: Boolean, val partial: Boolean = false)

data class HabitItem(
    val habit: Habit,
    val streaks: Streaks,
    /** The last 7 days, oldest first, ending today. */
    val week: List<DayCell>,
    val scheduledToday: Boolean,
    val doneToday: Boolean,
    /** Every completed day (local epoch days) and the habit's creation day, for the calendar. */
    val doneDays: Set<Long>,
    val startDay: Long,
    /** Today's slots (morning / noon / night) with their state; empty for a once-a-day habit. */
    val slots: List<SlotCell> = emptyList(),
)

data class HabitsState(
    val items: List<HabitItem> = emptyList(),
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val weekDone: Int = 0,
    val weekScheduled: Int = 0,
    /** Longest *current* streak among all habits. */
    val bestCurrentStreak: Int = 0,
    /** Today's pending items counting each day slot on its own (a habit with 2 slots counts 2). */
    val todaySlotsDone: Int = 0,
    val todaySlotsTotal: Int = 0,
    /** Today's local epoch day when the state was built. */
    val today: Long = 0,
) {
    val weekPercent: Int get() = if (weekScheduled == 0) 0 else Math.round(100f * weekDone / weekScheduled)
}

fun buildHabitsState(
    habits: List<Habit>,
    checks: List<HabitCheck>,
    today: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): HabitsState {
    val checksByHabit = checks.groupBy { it.habitId }
    var weekDone = 0
    var weekScheduled = 0
    val items = habits.map { habit ->
        val own = checksByHabit[habit.id].orEmpty()
        val done = completeDays(habit.slotsMask, own)
        val storedByDay = own.groupBy({ it.epochDay }, { it.slot }).mapValues { it.value.toSet() }
        val todayStored = storedByDay[today].orEmpty()
        val slotCells = slotsOf(habit.slotsMask).map { SlotCell(it, it.value in todayStored || 0 in todayStored) }
        val start = Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate().toEpochDay()
        val (wd, ws) = weekProgress(habit.daysMask, done, start, today)
        weekDone += wd
        weekScheduled += ws
        HabitItem(
            habit = habit,
            streaks = computeStreaks(habit.daysMask, done, start, today),
            week = (today - 6..today).map { day ->
                DayCell(day, isScheduled(habit.daysMask, day), day in done, day == today, partial = day !in done && day in storedByDay)
            },
            scheduledToday = isScheduled(habit.daysMask, today),
            doneToday = today in done,
            doneDays = done,
            startDay = start,
            slots = slotCells,
        )
    }
    val dueToday = items.filter { it.scheduledToday }
    return HabitsState(
        items = items,
        todayDone = dueToday.count { it.doneToday },
        todayTotal = dueToday.size,
        todaySlotsDone = dueToday.sumOf { item -> if (item.slots.isEmpty()) (if (item.doneToday) 1 else 0) else item.slots.count { it.done } },
        todaySlotsTotal = dueToday.sumOf { item -> maxOf(1, item.slots.size) },
        weekDone = weekDone,
        weekScheduled = weekScheduled,
        bestCurrentStreak = items.maxOfOrNull { it.streaks.current } ?: 0,
        today = today,
    )
}
