package com.cdiacal.tasks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cdiacal.tasks.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot (and may be after an update): re-create every pending reminder. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.get(context)
                val taskScheduler = ReminderScheduler(context)
                db.taskDao().pendingReminders(System.currentTimeMillis()).forEach(taskScheduler::sync)
                val habitScheduler = HabitReminderScheduler(context)
                db.habitDao().getAllHabits().forEach(habitScheduler::sync)
                DigestScheduler(context).sync()
            } finally {
                pending.finish()
            }
        }
    }
}
