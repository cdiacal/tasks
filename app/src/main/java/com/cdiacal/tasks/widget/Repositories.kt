package com.cdiacal.tasks.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.data.TaskRepository
import com.cdiacal.tasks.habits.HabitRepository
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import com.cdiacal.tasks.reminders.ReminderScheduler

/** Repository wired to refresh the home-screen widget after every change. */
fun createRepository(context: Context): TaskRepository {
    val app = context.applicationContext
    return TaskRepository(
        db = TaskDatabase.get(app),
        reminders = ReminderScheduler(app),
        habitReminders = HabitReminderScheduler(app),
        // A backup restore touches habits too, so both widgets are refreshed.
        onTasksChanged = {
            TasksWidget().updateAll(app)
            HabitsWidget().updateAll(app)
        },
    )
}

/** Habit repository wired to refresh the habits widget after every change. */
fun createHabitRepository(context: Context): HabitRepository {
    val app = context.applicationContext
    return HabitRepository(
        db = TaskDatabase.get(app),
        reminders = HabitReminderScheduler(app),
        onChanged = { HabitsWidget().updateAll(app) },
    )
}
