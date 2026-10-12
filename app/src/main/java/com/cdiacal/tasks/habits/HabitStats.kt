package com.cdiacal.tasks.habits

import java.time.Instant
import java.time.LocalDate
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

data class DayCell(val epochDay: Long, val scheduled: Boolean, val done: Boolean, val isToday: Boolean)

data class HabitItem(
    val habit: Habit,
    val streaks: Streaks,
    /** The last 7 days, oldest first, ending today. */
    val week: List<DayCell>,
    val scheduledToday: Boolean,
    val doneToday: Boolean,
)

data class HabitsState(
    val items: List<HabitItem> = emptyList(),
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val weekDone: Int = 0,
    val weekScheduled: Int = 0,
    /** Longest *current* streak among all habits. */
    val bestCurrentStreak: Int = 0,
) {
    val weekPercent: Int get() = if (weekScheduled == 0) 0 else Math.round(100f * weekDone / weekScheduled)
}

fun buildHabitsState(
    habits: List<Habit>,
    checks: List<HabitCheck>,
    today: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): HabitsState {
    val doneByHabit = checks.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
    var weekDone = 0
    var weekScheduled = 0
    val items = habits.map { habit ->
        val done = doneByHabit[habit.id].orEmpty()
        val start = Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate().toEpochDay()
        val (wd, ws) = weekProgress(habit.daysMask, done, start, today)
        weekDone += wd
        weekScheduled += ws
        HabitItem(
            habit = habit,
            streaks = computeStreaks(habit.daysMask, done, start, today),
            week = (today - 6..today).map { day ->
                DayCell(day, isScheduled(habit.daysMask, day), day in done, day == today)
            },
            scheduledToday = isScheduled(habit.daysMask, today),
            doneToday = today in done,
        )
    }
    val dueToday = items.filter { it.scheduledToday }
    return HabitsState(
        items = items,
        todayDone = dueToday.count { it.doneToday },
        todayTotal = dueToday.size,
        weekDone = weekDone,
        weekScheduled = weekScheduled,
        bestCurrentStreak = items.maxOfOrNull { it.streaks.current } ?: 0,
    )
}
