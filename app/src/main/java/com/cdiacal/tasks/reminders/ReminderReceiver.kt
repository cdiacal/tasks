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
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.widget.createRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires a task's reminder, and handles the notification's buttons: "Hecha" completes the task
 * (a repeating one is rescheduled), "+1 h" / "Mañana" move its reminder.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId < 0) return
        when (intent.action) {
            ACTION_DONE -> act(context, taskId) { task -> createRepository(context).toggleDone(task) }
            ACTION_SNOOZE_HOUR -> act(context, taskId) { task -> snooze(context, task, Snooze.HOUR) }
            ACTION_SNOOZE_TOMORROW -> act(context, taskId) { task -> snooze(context, task, Snooze.TOMORROW) }
            else -> show(context, taskId, intent.getStringExtra(EXTRA_TITLE).orEmpty())
        }
    }

    /** Runs a notification button's action off the main thread, and dismisses the notification. */
    private fun act(context: Context, taskId: Long, block: suspend (Task) -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
                val task = TaskDatabase.get(context).taskDao().getById(taskId) ?: return@launch
                // Already finished in the app since the notification appeared: nothing to do (and
                // "Hecha" must not reopen it).
                if (task.isDone) return@launch
                block(task)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun snooze(context: Context, task: Task, kind: Snooze) {
        createRepository(context).save(task.copy(reminderAt = snoozeUntil(kind, System.currentTimeMillis())))
    }

    private fun show(context: Context, taskId: Long, title: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags,
        )
        fun button(action: String) = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_TASK_ID, taskId),
            flags,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E4B87.toInt())
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.task_action_done), button(ACTION_DONE))
            .addAction(0, context.getString(R.string.task_action_snooze_hour), button(ACTION_SNOOZE_HOUR))
            .addAction(0, context.getString(R.string.task_action_snooze_tomorrow), button(ACTION_SNOOZE_TOMORROW))
            .build()
        // The permission was verified via areNotificationsEnabled() above.
        @Suppress("MissingPermission")
        manager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val ACTION_DONE = "com.cdiacal.tasks.TASK_DONE"
        const val ACTION_SNOOZE_HOUR = "com.cdiacal.tasks.TASK_SNOOZE_HOUR"
        const val ACTION_SNOOZE_TOMORROW = "com.cdiacal.tasks.TASK_SNOOZE_TOMORROW"
    }
}
