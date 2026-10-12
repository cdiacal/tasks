package com.cdiacal.tasks.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.SortedMap
import java.util.TreeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One occurrence of an event (a repeating event gives one of these per occurrence). */
data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String,
)

/**
 * The first and last local day [event] covers. All-day events are stored as UTC dates with an
 * exclusive end; a timed event ending exactly at midnight does not touch that next day.
 */
fun daysCovered(event: CalendarEvent, zone: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate> {
    val useZone = if (event.allDay) ZoneOffset.UTC else zone
    val first = Instant.ofEpochMilli(event.beginMillis).atZone(useZone).toLocalDate()
    val endInstant = Instant.ofEpochMilli(event.endMillis).atZone(useZone)
    var last = endInstant.toLocalDate()
    if (event.endMillis > event.beginMillis && endInstant.toLocalTime().toSecondOfDay() == 0) last = last.minusDays(1)
    return first to maxOf(first, last)
}

/**
 * Events grouped by the days they fall on, only for [from] until [from] + [days] days, each day
 * sorted all-day first and then by start time. A multi-day event appears on each of its days.
 */
fun groupByDay(
    events: List<CalendarEvent>,
    from: LocalDate,
    days: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): SortedMap<LocalDate, List<CalendarEvent>> {
    val lastDay = from.plusDays(days - 1L)
    val result = TreeMap<LocalDate, MutableList<CalendarEvent>>()
    for (event in events) {
        val (first, last) = daysCovered(event, zone)
        var day = maxOf(first, from)
        val stop = minOf(last, lastDay)
        while (!day.isAfter(stop)) {
            result.getOrPut(day) { mutableListOf() }.add(event)
            day = day.plusDays(1)
        }
    }
    result.values.forEach { list -> list.sortWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.beginMillis }) }
    return result
}

/** Read-only access to the calendars already synced on the phone (Google's included). */
object CalendarReader {
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Events from today (local) for the next [days] days. Empty without permission. */
    suspend fun upcoming(context: Context, days: Int, zone: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext emptyList()
            val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            // A day of slack on each side: all-day events are stored on UTC midnights.
            val begin = startOfToday - DAY_MILLIS
            val end = startOfToday + (days + 1) * DAY_MILLIS
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .also { ContentUris.appendId(it, begin); ContentUris.appendId(it, end) }
                .build()
            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Instances.STATUS,
            )
            val events = mutableListOf<CalendarEvent>()
            try {
                context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                    while (c.moveToNext()) {
                        if (!c.isNull(6) && c.getInt(6) == CalendarContract.Events.STATUS_CANCELED) continue
                        events += CalendarEvent(
                            eventId = c.getLong(0),
                            title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "",
                            beginMillis = c.getLong(2),
                            endMillis = c.getLong(3),
                            allDay = c.getInt(4) == 1,
                            calendarName = c.getString(5).orEmpty(),
                        )
                    }
                }
            } catch (_: SecurityException) {
                // Permission revoked while reading.
            }
            events
        }
}
