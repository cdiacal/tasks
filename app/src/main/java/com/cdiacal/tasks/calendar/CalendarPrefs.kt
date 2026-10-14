package com.cdiacal.tasks.calendar

import android.content.Context

/** Small local settings of the calendar import: which calendars are hidden and what was imported. */
object CalendarPrefs {
    private const val PREFS = "settings"
    private const val KEY_HIDDEN = "calendar_hidden"
    private const val KEY_IMPORTED = "calendar_imported"
    private const val KEY_NOTIFIED = "calendar_notified"
    private const val KEY_WATCH = "calendar_watch"
    private const val KEEP_MILLIS = 60L * 24 * 60 * 60 * 1000

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // The set returned by SharedPreferences must not be modified: always copy.
    fun hidden(context: Context): Set<String> = prefs(context).getStringSet(KEY_HIDDEN, emptySet())?.toSet().orEmpty()

    fun setHidden(context: Context, names: Set<String>) {
        prefs(context).edit().putStringSet(KEY_HIDDEN, names.toSet()).apply()
    }

    fun imported(context: Context): Set<String> = prefs(context).getStringSet(KEY_IMPORTED, emptySet())?.toSet().orEmpty()

    /** Events the daily check already told the user about, so it never repeats itself. */
    fun notified(context: Context): Set<String> = prefs(context).getStringSet(KEY_NOTIFIED, emptySet())?.toSet().orEmpty()

    fun addNotified(context: Context, keys: Set<String>, nowMillis: Long = System.currentTimeMillis()) {
        val all = pruneImported(notified(context) + keys, nowMillis - KEEP_MILLIS)
        prefs(context).edit().putStringSet(KEY_NOTIFIED, all).apply()
    }

    /** Whether the daily "events that look like tasks" notification is switched on (off by default). */
    fun watchEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_WATCH, false)

    fun setWatchEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WATCH, enabled).apply()
    }

    fun addImported(context: Context, keys: Set<String>, nowMillis: Long = System.currentTimeMillis()) {
        val all = pruneImported(imported(context) + keys, nowMillis - KEEP_MILLIS)
        prefs(context).edit().putStringSet(KEY_IMPORTED, all).apply()
    }
}
