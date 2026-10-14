package com.cdiacal.tasks.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.cdiacal.tasks.MainActivity
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.habits.DaySlot
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import com.cdiacal.tasks.habits.isDayComplete
import com.cdiacal.tasks.habits.isSlotDone
import com.cdiacal.tasks.habits.nextPendingSlot
import com.cdiacal.tasks.habits.slotEmoji
import com.cdiacal.tasks.habits.slotsOf
import com.cdiacal.tasks.ui.habits.slotLabelRes
import com.cdiacal.tasks.widget.HabitsWidget
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires one of a habit's daily reminders, or handles the notification's "Hecho" button.
 * A reminder is skipped when what it is about (the day, or its part of the day) is already done.
 */
class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        if (habitId < 0) return
        val slot = intent.getIntExtra(EXTRA_SLOT, 0)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TaskDatabase.get(context).habitDao()
                val today = LocalDate.now().toEpochDay()
                // A deleted habit has nothing to remind about and is not rescheduled.
                val habit = dao.getHabit(habitId) ?: return@launch
                val stored = dao.checksForDay(habitId, today).map { it.slot }.toSet()
                if (intent.action == ACTION_DONE) {
                    // «Hecho» ticks the reminder's own part of the day, or the next open one.
                    val target = if (slot > 0) slot.takeUnless { isSlotDone(it, stored) } else nextPendingSlot(habit.slotsMask, stored)
                    target?.let { dao.insertCheck(HabitCheck(habitId, today, it)) }
                    NotificationManagerCompat.from(context).cancel(notificationId(habitId, slot))
                    HabitsWidget().updateAll(context)
                } else {
                    val pendingNow = if (slot > 0) !isSlotDone(slot, stored) else !isDayComplete(habit.slotsMask, stored)
                    if (pendingNow) show(context, habit, slot)
                    HabitReminderScheduler(context).sync(habit) // schedule the next occurrence
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, habit: Habit, slot: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.habit_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val id = notificationId(habit.id, slot)
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
                .putExtra(EXTRA_HABIT_ID, habit.id)
                .putExtra(EXTRA_SLOT, slot),
            flags,
        )
        val title = listOf(habit.emoji, habit.name).filter { it.isNotBlank() }.joinToString(" ")
        val part = slotsOf(habit.slotsMask).firstOrNull { it.value == slot }
        val text = context.getString(R.string.habit_reminder_text).let {
            if (part == null) it else "${slotEmoji(part)} ${context.getString(slotLabelRes(part))} · $it"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E4B87.toInt())
            .setContentTitle(title)
            .setContentText(text)
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
        const val EXTRA_SLOT = "slot"
        const val ACTION_DONE = "com.cdiacal.tasks.HABIT_DONE"

        /** Negative so it can never collide with task notification ids (which are task ids). */
        fun notificationId(habitId: Long, slot: Int = 0): Int = -(habitId.toInt() * 4 + slot) - 1
    }
}
