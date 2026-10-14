package com.cdiacal.tasks.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.nextReminderMillis
import com.cdiacal.tasks.habits.reminderTimes

/**
 * One pending alarm per reminder of a habit (a plain habit has one, a habit with day slots up to
 * three), always for its *next* occurrence. When it fires, the receiver shows the notification and
 * schedules the following one, so no repeating alarm is needed.
 */
class HabitReminderScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(AlarmManager::class.java)

    /** Schedules the next reminders and cancels the ones the habit no longer has. */
    fun sync(habit: Habit) {
        val wanted = reminderTimes(habit).toMap()
        val now = System.currentTimeMillis()
        for (slot in SLOTS) {
            val next = wanted[slot]?.let { nextReminderMillis(habit.daysMask, it, now) }
            if (next == null) {
                alarmManager.cancel(pendingIntent(habit.id, slot))
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent(habit.id, slot))
            }
        }
    }

    fun cancel(habitId: Long) {
        SLOTS.forEach { alarmManager.cancel(pendingIntent(habitId, it)) }
    }

    private fun pendingIntent(habitId: Long, slot: Int): PendingIntent {
        val intent = Intent(app, HabitReminderReceiver::class.java)
            .putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            .putExtra(HabitReminderReceiver.EXTRA_SLOT, slot)
        // Slot 0 keeps the request code of earlier versions, so their alarms are replaced, not duplicated.
        val requestCode = if (slot == 0) habitId.hashCode() else SLOT_BASE + habitId.toInt() * 4 + slot
        return PendingIntent.getBroadcast(
            app,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        val SLOTS = 0..3
        const val SLOT_BASE = 100_000_000
    }
}
