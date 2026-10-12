package com.cdiacal.tasks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cdiacal.tasks.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot, so re-create them for every pending future reminder. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(context)
                TaskDatabase.get(context).taskDao()
                    .pendingReminders(System.currentTimeMillis())
                    .forEach(scheduler::sync)
            } finally {
                pending.finish()
            }
        }
    }
}
