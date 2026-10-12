package com.cdiacal.tasks.reminders

import java.time.Instant
import java.time.ZoneId

enum class Snooze { HOUR, TOMORROW }

/**
 * When a snoozed task reminder should fire again: one hour from now, or tomorrow at 09:00 local
 * time (always the next calendar day, even when snoozed late at night).
 */
fun snoozeUntil(kind: Snooze, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long = when (kind) {
    Snooze.HOUR -> nowMillis + 60L * 60 * 1000
    Snooze.TOMORROW -> Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        .plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
}
