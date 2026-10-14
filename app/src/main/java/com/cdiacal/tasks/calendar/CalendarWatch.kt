package com.cdiacal.tasks.calendar

import android.app.AlarmManager
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
import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.nextReminderMillis
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHECK_MINUTES = 8 * 60 + 30 // 08:30 every day
private const val LOOKAHEAD_DAYS = 30

/** One alarm a day that runs [CalendarWatchReceiver]; it is only scheduled while the option is on. */
class CalendarWatchScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(AlarmManager::class.java)

    fun sync() {
        val next = if (CalendarPrefs.watchEnabled(app)) {
            nextReminderMillis(ALL_DAYS, CHECK_MINUTES, System.currentTimeMillis())
        } else {
            null
        }
        if (next == null) {
            alarmManager.cancel(pendingIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent())
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        app,
        REQUEST_CODE,
        Intent(app, CalendarWatchReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 7002
    }
}

/**
 * Looks at the next 30 days of the phone's calendars and, when there are events that look like
 * tasks and were not announced before, shows one notification that opens the import screen.
 */
class CalendarWatchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (CalendarPrefs.watchEnabled(context) && CalendarReader.hasPermission(context)) {
                    val today = LocalDate.now()
                    val candidates = newTaskCandidates(
                        events = CalendarReader.upcoming(context, LOOKAHEAD_DAYS),
                        hidden = CalendarPrefs.hidden(context),
                        imported = CalendarPrefs.imported(context),
                        notified = CalendarPrefs.notified(context),
                        today = today,
                    )
                    if (candidates.isNotEmpty() && show(context, candidates)) {
                        CalendarPrefs.addNotified(context, candidates.map { eventKey(it.first) }.toSet())
                    }
                }
                CalendarWatchScheduler(context).sync()
            } finally {
                pending.finish()
            }
        }
    }

    /** Returns whether the notification was really posted (it is not when notifications are off). */
    private fun show(context: Context, candidates: List<Pair<CalendarEvent, TaskSuggestion>>): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.calendar_watch_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val summary = context.resources.getQuantityString(R.plurals.calendar_watch_summary, candidates.size, candidates.size)
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val shown = candidates.take(MAX_LINES)
        val details = buildString {
            append(summary)
            shown.forEach { (_, suggestion) -> append("\n• ").append(suggestion.title).append(" · ").append(dateFormat.format(suggestion.due)) }
            if (candidates.size > shown.size) append("\n").append(context.getString(R.string.digest_more, candidates.size - shown.size))
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_CALENDAR_IMPORT, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E4B87.toInt())
            .setContentTitle(context.getString(R.string.calendar_watch_title))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        // The permission was verified via areNotificationsEnabled() above.
        @Suppress("MissingPermission")
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private companion object {
        const val CHANNEL_ID = "calendar_watch"
        const val MAX_LINES = 5
        /** Negative so it never collides with task ids (positive) or the daily summary (-1_000_000). */
        const val NOTIFICATION_ID = -2_000_000
    }
}
