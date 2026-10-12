package com.cdiacal.tasks.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class Priority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    ;

    companion object {
        /** Unknown levels (e.g. from a hand-edited backup) mean "no priority". */
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

fun Task.priorityKind(): Priority = Priority.fromLevel(priority)

enum class TaskSort { DATE, PRIORITY }

/**
 * [TaskSort.DATE] keeps the database order (open tasks by nearest due date). [TaskSort.PRIORITY]
 * puts higher priority first; the sort is stable, so inside a priority the date order remains and
 * done tasks stay at the end.
 */
fun List<Task>.sortedFor(sort: TaskSort): List<Task> = when (sort) {
    TaskSort.DATE -> this
    TaskSort.PRIORITY -> sortedWith(compareBy<Task> { it.isDone }.thenByDescending { it.priority })
}

/** The chosen list order, shared by the app and the widget (hence preferences, observed as a Flow). */
object TaskSortStore {
    private const val PREFS = "settings"
    private const val KEY = "task_sort"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): TaskSort =
        TaskSort.entries.firstOrNull { it.name == prefs(context).getString(KEY, null) } ?: TaskSort.DATE

    fun set(context: Context, sort: TaskSort) {
        prefs(context).edit().putString(KEY, sort.name).apply()
    }

    fun flow(context: Context): Flow<TaskSort> = callbackFlow {
        val prefs = prefs(context)
        // SharedPreferences holds listeners weakly: awaitClose below keeps this one referenced.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) trySend(get(context))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(get(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
