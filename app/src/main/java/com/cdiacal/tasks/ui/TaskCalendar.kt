package com.cdiacal.tasks.ui

import com.cdiacal.tasks.data.Priority
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.priorityKind
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** The day a task is due; due dates are stored as UTC midnights. Null for a task without one. */
fun Task.dueLocalDate(): LocalDate? =
    dueDate?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

/** One square of the month grid. */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val pending: Int,
    val done: Int,
    /** The highest priority among the pending tasks of the day. */
    val topPriority: Priority,
    /** A day before today that still has pending tasks. */
    val overdue: Boolean,
)

/** Weeks (Monday first) covering [month], with what is due on each day. */
fun buildTaskMonth(month: YearMonth, tasks: List<Task>, today: LocalDate): List<List<CalendarDay>> {
    val byDay = tasks.mapNotNull { task -> task.dueLocalDate()?.let { it to task } }.groupBy({ it.first }, { it.second })
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val weeks = ((first.dayOfWeek.value - 1) + month.lengthOfMonth() + 6) / 7
    return List(weeks) { week ->
        List(7) { dayInWeek ->
            val date = start.plusDays(week * 7L + dayInWeek)
            val dayTasks = byDay[date].orEmpty()
            val pending = dayTasks.filter { !it.isDone }
            CalendarDay(
                date = date,
                inMonth = YearMonth.from(date) == month,
                pending = pending.size,
                done = dayTasks.size - pending.size,
                topPriority = pending.maxOfOrNull { it.priority }?.let { Priority.fromLevel(it) } ?: Priority.NONE,
                overdue = date.isBefore(today) && pending.isNotEmpty(),
            )
        }
    }
}

/** Tasks due on [date]: open ones first (highest priority first), then the done ones. */
fun tasksOn(date: LocalDate, tasks: List<Task>): List<Task> =
    tasks.filter { it.dueLocalDate() == date }
        .sortedWith(compareBy<Task> { it.isDone }.thenByDescending { it.priority }.thenBy { it.createdAt })
