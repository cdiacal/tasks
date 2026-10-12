package com.cdiacal.tasks.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Adds [id] if absent, removes it if present. */
internal fun toggleId(ids: Set<Long>, id: Long): Set<Long> = if (id in ids) ids - id else ids + id

/**
 * Which tasks have their steps unfolded in the home-screen widget. Kept in preferences (the widget
 * is redrawn from scratch at times, so this must outlive a single composition) and exposed as a
 * [Flow] so the widget recomposes the moment a task is folded or unfolded.
 */
object WidgetExpansion {
    private const val PREFS = "widget_state"
    private const val KEY = "expanded_tasks"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun expanded(context: Context): Set<Long> =
        prefs(context).getStringSet(KEY, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun toggle(context: Context, taskId: Long) {
        val updated = toggleId(expanded(context), taskId)
        prefs(context).edit().putStringSet(KEY, updated.map { it.toString() }.toSet()).apply()
    }

    fun flow(context: Context): Flow<Set<Long>> = callbackFlow {
        val prefs = prefs(context)
        // SharedPreferences holds listeners weakly: awaitClose below keeps this one referenced.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) trySend(expanded(context))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(expanded(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
