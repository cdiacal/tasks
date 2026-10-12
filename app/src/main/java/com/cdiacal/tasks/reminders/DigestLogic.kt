package com.cdiacal.tasks.reminders

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.ui.DueState
import com.cdiacal.tasks.ui.dueState

/** What the daily summary has to say. */
data class Digest(
    val dueToday: Int,
    val overdue: Int,
    val habitsPending: Int,
    /** Titles of the overdue then due-today tasks, capped. */
    val titles: List<String>,
    /** How many more titles did not fit in [titles]. */
    val moreTitles: Int,
) {
    /** Nothing to report: the summary is then not shown at all. */
    val isEmpty: Boolean get() = dueToday + overdue + habitsPending == 0
}

/** [todayUtc] is today's local date as UTC midnight millis, like [Task.dueDate]. */
fun computeDigest(tasks: List<Task>, habits: HabitsState, todayUtc: Long, maxTitles: Int = 5): Digest {
    val open = tasks.filter { !it.isDone }
    // Higher priority first, then oldest first, so a cap keeps the most important titles.
    val byImportance = compareByDescending<Task> { it.priority }.thenBy { it.dueDate }
    val overdue = open.filter { dueState(it, todayUtc) == DueState.OVERDUE }.sortedWith(byImportance)
    val dueToday = open.filter { it.dueDate == todayUtc }.sortedWith(byImportance)
    val titles = (overdue + dueToday).map { it.title }
    return Digest(
        dueToday = dueToday.size,
        overdue = overdue.size,
        habitsPending = habits.todayTotal - habits.todayDone,
        titles = titles.take(maxTitles),
        moreTitles = (titles.size - maxTitles).coerceAtLeast(0),
    )
}
