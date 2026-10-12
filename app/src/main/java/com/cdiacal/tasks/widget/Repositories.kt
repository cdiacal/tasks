package com.cdiacal.tasks.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.data.TaskRepository
import com.cdiacal.tasks.reminders.ReminderScheduler

/** Repository wired to refresh the home-screen widget after every change. */
fun createRepository(context: Context): TaskRepository {
    val app = context.applicationContext
    return TaskRepository(
        db = TaskDatabase.get(app),
        reminders = ReminderScheduler(app),
        onTasksChanged = { TasksWidget().updateAll(app) },
    )
}
