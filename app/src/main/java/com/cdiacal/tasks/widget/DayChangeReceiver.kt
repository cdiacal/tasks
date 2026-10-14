package com.cdiacal.tasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.cdiacal.tasks.calendar.CalendarWatchScheduler
import com.cdiacal.tasks.reminders.DigestScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Both widgets depend on "today" (due dates, which habits are scheduled), so redraw them when the
 * date, the clock or the time zone changes instead of waiting for the periodic refresh.
 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TasksWidget().updateAll(app)
                HabitsWidget().updateAll(app)
                // A time-zone or clock change moves "08:00": recompute the summary alarm.
                DigestScheduler(app).sync()
                CalendarWatchScheduler(app).sync()
            } finally {
                pending.finish()
            }
        }
    }
}
