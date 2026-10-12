package com.cdiacal.tasks.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cdiacal.tasks.MainActivity
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires a habit's daily reminder, or handles the notification's "Hecho" button.
 * Reminders are skipped when the habit is already done today.
 */
class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        if (habitId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TaskDatabase.get(context).habitDao()
                val today = LocalDate.now().toEpochDay()
                if (intent.action == ACTION_DONE) {
                    dao.insertCheck(HabitCheck(habitId, today))
                    NotificationManagerCompat.from(context).cancel(notificationId(habitId))
                } else {
                    // A deleted habit has nothing to remind about and is not rescheduled.
                    val habit = dao.getHabit(habitId) ?: return@launch
                    if (dao.isChecked(habitId, today) == 0) show(context, habit)
                    HabitReminderScheduler(context).sync(habit) // schedule the next occurrence
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, habit: Habit) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.habit_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val id = notificationId(habit.id)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_HABITS, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags,
        )
        val done = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, HabitReminderReceiver::class.java)
                .setAction(ACTION_DONE)
                .putExtra(EXTRA_HABIT_ID, habit.id),
            flags,
        )
        val title = listOf(habit.emoji, habit.name).filter { it.isNotBlank() }.joinToString(" ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E4B87.toInt())
            .setContentTitle(title)
            .setContentText(context.getString(R.string.habit_reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.habit_done_action), done)
            .build()
        // The permission was verified via areNotificationsEnabled() above.
        @Suppress("MissingPermission")
        manager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val EXTRA_HABIT_ID = "habit_id"
        const val ACTION_DONE = "com.cdiacal.tasks.HABIT_DONE"

        /** Negative so it can never collide with task notification ids (which are task ids). */
        fun notificationId(habitId: Long): Int = -habitId.toInt() - 1
    }
}
