package com.cdiacal.tasks.habits

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** "08:05" for 485 minutes after midnight. */
fun formatMinutes(minutesOfDay: Int): String = "%02d:%02d".format(minutesOfDay / 60, minutesOfDay % 60)

/**
 * The next moment (epoch millis, strictly after [nowMillis]) at which a daily habit reminder
 * should fire: [minutesOfDay] local time, on the next scheduled weekday. Null when the habit
 * has no scheduled days.
 */
fun nextReminderMillis(
    daysMask: Int,
    minutesOfDay: Int,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long? {
    if (daysMask and ALL_DAYS == 0) return null
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val time = LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
    // A weekly habit needs up to 7 days ahead (today counts when the time is still to come).
    for (offset in 0L..7L) {
        val date = today.plusDays(offset)
        if (!isScheduled(daysMask, date.toEpochDay())) continue
        val candidate = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (candidate > nowMillis) return candidate
    }
    return null
}
