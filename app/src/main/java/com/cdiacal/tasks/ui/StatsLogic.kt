package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskCompletion
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import com.cdiacal.tasks.habits.completeDays
import com.cdiacal.tasks.habits.computeStreaks
import com.cdiacal.tasks.habits.dayIndex
import com.cdiacal.tasks.habits.historyStart
import com.cdiacal.tasks.habits.isScheduled
import java.time.Instant
import java.time.ZoneId

/** Number of weeks the charts cover (the current, still running one included). */
const val STATS_WEEKS = 8

/** Monday of the week that holds [epochDay]. */
fun weekStart(epochDay: Long): Long = epochDay - dayIndex(epochDay)

/** Completions per week, oldest first; the last entry is the current week. */
fun weeklyCounts(days: List<Long>, today: Long, weeks: Int = STATS_WEEKS): List<Int> {
    val firstWeek = weekStart(today) - 7L * (weeks - 1)
    val counts = IntArray(weeks)
    for (day in days) {
        if (day < firstWeek || day > today) continue
        counts[((day - firstWeek) / 7).toInt()]++
    }
    return counts.toList()
}

/** (done, scheduled) for one habit over the days of [from]..[to] that it existed. */
private fun habitRange(habit: Habit, done: Set<Long>, startDay: Long, from: Long, to: Long): Pair<Int, Int> {
    val first = maxOf(from, historyStart(startDay, done))
    var completed = 0
    var scheduled = 0
    for (day in first..to) {
        if (!isScheduled(habit.daysMask, day)) continue
        scheduled++
        if (day in done) completed++
    }
    return completed to scheduled
}

data class HabitStreakRow(val habit: Habit, val current: Int, val best: Int)

data class StatsState(
    val today: Long = 0,
    /** Tasks completed per week, oldest first. */
    val taskWeeks: List<Int> = List(STATS_WEEKS) { 0 },
    val completedThisWeek: Int = 0,
    val completedLast30: Int = 0,
    val completedTotal: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    /** Habit completion rate per week as 0..100, or null when nothing was scheduled that week. */
    val habitWeeks: List<Int?> = List(STATS_WEEKS) { null },
    /** Same rate over the last 30 days, or null without any scheduled day. */
    val habitRate30: Int? = null,
    val habitRows: List<HabitStreakRow> = emptyList(),
)

private fun percent(done: Int, scheduled: Int): Int? =
    if (scheduled == 0) null else Math.round(100f * done / scheduled)

fun buildStats(
    tasks: List<Task>,
    completions: List<TaskCompletion>,
    habits: List<Habit>,
    checks: List<HabitCheck>,
    today: Long,
    todayUtcMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): StatsState {
    val days = completions.map { it.epochDay }
    val taskWeeks = weeklyCounts(days, today)
    val taskStats = computeStats(tasks, todayUtcMillis)

    val checksByHabit = checks.groupBy { it.habitId }
    val firstWeek = weekStart(today) - 7L * (STATS_WEEKS - 1)
    val weekDone = IntArray(STATS_WEEKS)
    val weekScheduled = IntArray(STATS_WEEKS)
    var done30 = 0
    var scheduled30 = 0
    val rows = habits.map { habit ->
        val done = completeDays(habit.slotsMask, checksByHabit[habit.id].orEmpty())
        val start = Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate().toEpochDay()
        for (w in 0 until STATS_WEEKS) {
            val from = firstWeek + 7L * w
            val (d, s) = habitRange(habit, done, start, from, minOf(from + 6, today))
            weekDone[w] += d
            weekScheduled[w] += s
        }
        val (d30, s30) = habitRange(habit, done, start, today - 29, today)
        done30 += d30
        scheduled30 += s30
        val streaks = computeStreaks(habit.daysMask, done, start, today)
        HabitStreakRow(habit, streaks.current, streaks.best)
    }
    return StatsState(
        today = today,
        taskWeeks = taskWeeks,
        completedThisWeek = taskWeeks.last(),
        completedLast30 = days.count { it in (today - 29)..today },
        completedTotal = days.size,
        pending = taskStats.pending,
        overdue = taskStats.overdue,
        habitWeeks = List(STATS_WEEKS) { percent(weekDone[it], weekScheduled[it]) },
        habitRate30 = percent(done30, scheduled30),
        habitRows = rows.sortedWith(compareByDescending<HabitStreakRow> { it.current }.thenByDescending { it.best }),
    )
}
