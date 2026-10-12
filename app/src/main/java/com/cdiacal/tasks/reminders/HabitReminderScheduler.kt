package com.cdiacal.tasks.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.nextReminderMillis

/**
 * One pending alarm per habit, always for its *next* occurrence. When it fires, the receiver
 * shows the notification and schedules the following one, so no repeating alarm is needed.
 */
class HabitReminderScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(AlarmManager::class.java)

    /** Schedules the next reminder, or cancels it when the habit has none. */
    fun sync(habit: Habit) {
        val minutes = habit.reminderMinutes
        val next = if (minutes == null) null else nextReminderMillis(habit.daysMask, minutes, System.currentTimeMillis())
        if (next == null) {
            cancel(habit.id)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent(habit.id))
        }
    }

    fun cancel(habitId: Long) {
        alarmManager.cancel(pendingIntent(habitId))
    }

    private fun pendingIntent(habitId: Long): PendingIntent {
        val intent = Intent(app, HabitReminderReceiver::class.java)
            .putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        return PendingIntent.getBroadcast(
            app,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
