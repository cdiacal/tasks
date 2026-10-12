package com.cdiacal.tasks.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cdiacal.tasks.MainActivity
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.ui.DueState
import com.cdiacal.tasks.ui.dueState
import com.cdiacal.tasks.ui.formatDate
import com.cdiacal.tasks.ui.reminderToUtcDateMillis

// Widgets can't use the app theme, so the design system defines fixed colors for them.
private val TextWhite = ColorProvider(Color(0xFFFFFFFF))
private val TextDim = ColorProvider(Color(0xFFD2DCEC))
private val Green = ColorProvider(Color(0xFF5FD9A0))
private val Red = ColorProvider(Color(0xFFF0857A))

private val TaskIdKey = ActionParameters.Key<Long>("task_id")

class TasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = TaskDatabase.get(context).taskDao()
        val tasks = dao.pending(limit = 30)
        val count = dao.pendingCount()
        val today = reminderToUtcDateMillis(System.currentTimeMillis())
        provideContent { WidgetContent(tasks, count, today) }
    }
}

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}

/** Marks a task as done from the widget (cancels its reminder and refreshes the widget). */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val task = TaskDatabase.get(context).taskDao().getById(id) ?: return
        createRepository(context).save(task.copy(isDone = true))
    }
}

/** Opens the app. Uses the Intent overload, which is the one that resolves in Glance 1.1.0. */
@Composable
private fun openApp(): Action = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

@Composable
private fun WidgetContent(tasks: List<Task>, count: Int, today: Long) {
    val context = LocalContext.current
    val newTask = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_NEW_TASK, true)

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().clickable(openApp())) {
                Text(
                    context.getString(R.string.app_name),
                    style = TextStyle(color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    context.getString(R.string.widget_pending_count, count),
                    style = TextStyle(color = TextDim, fontSize = 12.sp),
                )
            }
            Box(
                GlanceModifier
                    .size(36.dp)
                    .background(ImageProvider(R.drawable.widget_add_background))
                    .clickable(actionStartActivity(newTask)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            }
        }

        if (tasks.isEmpty()) {
            Box(GlanceModifier.fillMaxSize().clickable(openApp()), contentAlignment = Alignment.Center) {
                Text(
                    context.getString(R.string.widget_all_done),
                    style = TextStyle(color = Green, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(tasks, itemId = { it.id }) { task -> TaskRow(task, today) }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, today: Long) {
    val overdue = dueState(task, today) == DueState.OVERDUE
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        // Larger tap target than the visible 20 dp circle.
        Box(
            GlanceModifier
                .size(40.dp)
                .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to task.id))),
            contentAlignment = Alignment.Center,
        ) {
            Box(GlanceModifier.size(20.dp).background(ImageProvider(R.drawable.widget_check_background))) {}
        }
        Spacer(GlanceModifier.width(4.dp))
        Column(GlanceModifier.defaultWeight().clickable(openApp())) {
            Text(task.title, style = TextStyle(color = TextWhite, fontSize = 14.sp), maxLines = 1)
            task.dueDate?.let {
                Text(
                    formatDate(it),
                    style = TextStyle(color = if (overdue) Red else TextDim, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
