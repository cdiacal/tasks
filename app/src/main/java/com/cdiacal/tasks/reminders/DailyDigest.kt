package com.cdiacal.tasks.reminders

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
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.habits.ALL_DAYS
import com.cdiacal.tasks.habits.buildHabitsState
import com.cdiacal.tasks.habits.nextReminderMillis
import com.cdiacal.tasks.ui.reminderToUtcDateMillis
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class DigestConfig(
    val enabled: Boolean = false,
    /** Time of day as minutes after local midnight (default 08:00). */
    val minutes: Int = 8 * 60,
)

/** Daily-summary settings, stored with the app's other preferences. */
object DigestSettings {
    private const val PREFS = "settings"
    private const val KEY_ENABLED = "digest_enabled"
    private const val KEY_MINUTES = "digest_minutes"

    fun load(context: Context): DigestConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DigestConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            minutes = prefs.getInt(KEY_MINUTES, 8 * 60),
        )
    }

    fun save(context: Context, config: DigestConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_MINUTES, config.minutes)
            .apply()
    }
}

/** One pending alarm for the next summary; the receiver schedules the one after it. */
class DigestScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(AlarmManager::class.java)

    /** Schedules (or cancels) the alarm to match the saved settings. Safe to call repeatedly. */
    fun sync() {
        val config = DigestSettings.load(app)
        val next = if (config.enabled) nextReminderMillis(ALL_DAYS, config.minutes, System.currentTimeMillis()) else null
        if (next == null) {
            alarmManager.cancel(pendingIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent())
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        app,
        REQUEST_CODE,
        Intent(app, DigestReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 7001
    }
}

/** Builds and shows the morning summary, then schedules tomorrow's. Silent when there is nothing to say. */
class DigestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (DigestSettings.load(context).enabled) {
                    val db = TaskDatabase.get(context)
                    val habits = buildHabitsState(
                        db.habitDao().getAllHabits(),
                        db.habitDao().getAllChecks(),
                        LocalDate.now().toEpochDay(),
                    )
                    val digest = computeDigest(
                        db.taskDao().getAll(),
                        habits,
                        reminderToUtcDateMillis(System.currentTimeMillis()),
                    )
                    if (!digest.isEmpty) show(context, digest)
                }
                DigestScheduler(context).sync()
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, digest: Digest) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.digest_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val res = context.resources
        val summary = buildList {
            if (digest.dueToday > 0) add(res.getQuantityString(R.plurals.digest_tasks, digest.dueToday, digest.dueToday))
            if (digest.overdue > 0) add(res.getQuantityString(R.plurals.digest_overdue, digest.overdue, digest.overdue))
            if (digest.habitsPending > 0) {
                add(res.getQuantityString(R.plurals.digest_habits, digest.habitsPending, digest.habitsPending))
            }
        }.joinToString(" · ")
        val details = buildString {
            append(summary)
            digest.titles.forEach { append("\n• ").append(it) }
            if (digest.moreTitles > 0) append("\n").append(res.getString(R.string.digest_more, digest.moreTitles))
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E4B87.toInt())
            .setContentTitle(context.getString(R.string.digest_title))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        // The permission was verified via areNotificationsEnabled() above.
        @Suppress("MissingPermission")
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "daily_digest"
        /** Negative so it can never collide with task ids (positive) or habit ids (small negatives). */
        const val NOTIFICATION_ID = -1_000_000
    }
}
