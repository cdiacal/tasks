package com.cdiacal.tasks.ui

import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/** Combines a Material date-picker value (UTC midnight millis) with a local wall-clock time. */
fun combineReminder(
    utcDateMillis: Long,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val date = Instant.ofEpochMilli(utcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

/** Inverse of [combineReminder] for the date part: the local date as UTC midnight millis. */
fun reminderToUtcDateMillis(reminderAt: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val date: LocalDate = Instant.ofEpochMilli(reminderAt).atZone(zone).toLocalDate()
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun reminderHour(reminderAt: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(reminderAt).atZone(zone).hour

fun reminderMinute(reminderAt: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(reminderAt).atZone(zone).minute

fun formatReminder(reminderAt: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(reminderAt))
