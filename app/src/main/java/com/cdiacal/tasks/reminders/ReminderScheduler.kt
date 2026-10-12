package com.cdiacal.tasks.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cdiacal.tasks.data.Task

/**
 * Schedules one alarm per task. Uses an inexact-but-idle-friendly alarm so no special
 * "exact alarm" permission is needed; delivery can be a few minutes late in Doze mode.
 */
class ReminderScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(AlarmManager::class.java)

    /** Schedules the reminder if it is in the future and the task is pending; cancels it otherwise. */
    fun sync(task: Task) {
        val at = task.reminderAt
        if (at != null && !task.isDone && at > System.currentTimeMillis()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(task.id, task.title))
        } else {
            cancel(task.id)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId, ""))
    }

    private fun pendingIntent(taskId: Long, title: String): PendingIntent {
        val intent = Intent(app, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            app,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
