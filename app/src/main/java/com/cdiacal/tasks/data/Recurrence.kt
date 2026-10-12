package com.cdiacal.tasks.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    fun next(date: LocalDate): LocalDate = when (this) {
        NONE -> date
        DAILY -> date.plusDays(1)
        WEEKLY -> date.plusWeeks(1)
        MONTHLY -> date.plusMonths(1)
        YEARLY -> date.plusYears(1)
    }

    companion object {
        /** Unknown or missing names (e.g. from a hand-edited backup) mean "does not repeat". */
        fun fromName(name: String?): Recurrence = entries.firstOrNull { it.name == name } ?: NONE
    }
}

fun Task.recurrenceKind(): Recurrence = Recurrence.fromName(recurrence)

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * The next due date (UTC-midnight millis, like [Task.dueDate]) for a repeating task completed on
 * [todayUtc]. It is one period after the current due date, or, when the task is overdue, the first
 * occurrence after today, so a weekly task completed late does not come back already overdue.
 */
fun nextDueDate(due: Long, kind: Recurrence, todayUtc: Long): Long {
    if (kind == Recurrence.NONE) return due
    val today = LocalDate.ofEpochDay(Math.floorDiv(todayUtc, DAY_MILLIS))
    var date = LocalDate.ofEpochDay(Math.floorDiv(due, DAY_MILLIS))
    do {
        date = kind.next(date)
    } while (!date.isAfter(today))
    return date.toEpochDay() * DAY_MILLIS
}

/** Moves an instant by whole calendar days, keeping the local wall-clock time across DST changes. */
fun shiftByDays(millis: Long, days: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis).atZone(zone).plusDays(days).toInstant().toEpochMilli()

/**
 * What completing [task] produces: a repeating task moves to its next date (and its reminder moves
 * with it) and stays open; any other task becomes done.
 */
fun completeTask(task: Task, todayUtc: Long, zone: ZoneId = ZoneId.systemDefault()): Task {
    val kind = task.recurrenceKind()
    val due = task.dueDate
    if (kind == Recurrence.NONE || due == null) return task.copy(isDone = true)
    val next = nextDueDate(due, kind, todayUtc)
    val shiftedDays = (next - due) / DAY_MILLIS
    return task.copy(
        dueDate = next,
        reminderAt = task.reminderAt?.let { shiftByDays(it, shiftedDays, zone) },
        isDone = false,
    )
}

/**
 * [task] pushed to tomorrow (relative to [todayUtc]); its reminder moves by the same number of
 * days. A task without a due date is returned unchanged.
 */
fun postponeToTomorrow(task: Task, todayUtc: Long, zone: ZoneId = ZoneId.systemDefault()): Task {
    val due = task.dueDate ?: return task
    val tomorrow = todayUtc + DAY_MILLIS
    val shiftedDays = (tomorrow - due) / DAY_MILLIS
    return task.copy(
        dueDate = tomorrow,
        reminderAt = task.reminderAt?.let { shiftByDays(it, shiftedDays, zone) },
    )
}
