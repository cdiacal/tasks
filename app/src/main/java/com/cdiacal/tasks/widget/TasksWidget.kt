package com.cdiacal.tasks.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.glance.appwidget.updateAll
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
import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.TaskSortStore
import com.cdiacal.tasks.data.priorityKind
import com.cdiacal.tasks.data.sortedFor
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.ui.DueState
import com.cdiacal.tasks.ui.dueState
import com.cdiacal.tasks.ui.formatDate
import com.cdiacal.tasks.ui.priorityMarker
import com.cdiacal.tasks.ui.reminderToUtcDateMillis

// Widgets can't use the app theme, so the design system defines fixed colors for them.
internal val TextWhite = ColorProvider(Color(0xFFFFFFFF))
internal val TextDim = ColorProvider(Color(0xFFD2DCEC))
internal val Green = ColorProvider(Color(0xFF5FD9A0))
internal val Red = ColorProvider(Color(0xFFF0857A))

// Sorting by priority happens after the query, so fetch enough rows for it to matter.
private const val WIDGET_TASK_LIMIT = 60

private val TaskIdKey = ActionParameters.Key<Long>("task_id")
private val StepIdKey = ActionParameters.Key<Long>("step_id")

class TasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = TaskDatabase.get(context)
        val dao = db.taskDao()
        val stepDao = db.subtaskDao()
        val initialSteps = stepDao.getAll()
        // Read once so the very first frame is already correct (no empty flash)...
        val initialTasks = dao.pending(limit = WIDGET_TASK_LIMIT)
        val initialCount = dao.pendingCount()
        provideContent {
            // ...then keep observing the database: the content is recomposed by itself on every
            // change. Data read once outside provideContent would stay stale if Glance reuses
            // the running session for an update.
            val tasks by dao.observePending(WIDGET_TASK_LIMIT).collectAsState(initialTasks)
            val count by dao.observePendingCount().collectAsState(initialCount)
            val steps by stepDao.observeAll().collectAsState(initialSteps)
            val sort by TaskSortStore.flow(context).collectAsState(TaskSortStore.get(context))
            val shown = remember(tasks, sort) { tasks.sortedFor(sort) }
            // Which tasks are unfolded is observed too, so unfolding redraws at once.
            val expanded by WidgetExpansion.flow(context).collectAsState(WidgetExpansion.expanded(context))
            val stepsByTask = remember(steps) { steps.groupBy { it.taskId } }
            val today = reminderToUtcDateMillis(System.currentTimeMillis())
            WidgetContent(shown, count, today, stepsByTask, expanded)
        }
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
        createRepository(context).toggleDone(task)
    }
}

/** Folds / unfolds a task's steps in the widget. */
class ToggleExpandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        WidgetExpansion.toggle(context, id)
        TasksWidget().updateAll(context) // also covers a widget session that has already ended
    }
}

/** Ticks / unticks one step from the widget. */
class ToggleStepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[StepIdKey] ?: return
        createRepository(context).toggleStep(id)
    }
}

/** Opens the app. Uses the Intent overload, which is the one that resolves in Glance 1.1.0. */
@Composable
private fun openApp(): Action = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

@Composable
private fun WidgetContent(
    tasks: List<Task>,
    count: Int,
    today: Long,
    stepsByTask: Map<Long, List<Subtask>>,
    expanded: Set<Long>,
) {
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
                items(tasks, itemId = { it.id }) { task ->
                    TaskRow(task, today, stepsByTask[task.id].orEmpty(), task.id in expanded)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, today: Long, steps: List<Subtask>, expanded: Boolean) {
    val overdue = dueState(task, today) == DueState.OVERDUE
    Column(GlanceModifier.fillMaxWidth()) {
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
            val line = listOfNotNull(
                priorityMarker(task.priorityKind()).takeIf { it.isNotEmpty() },
                task.dueDate?.let { formatDate(it) },
            ).joinToString(" · ")
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = TextStyle(color = if (overdue) Red else TextDim, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
        if (steps.isNotEmpty()) {
            Box(
                GlanceModifier
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .clickable(actionRunCallback<ToggleExpandAction>(actionParametersOf(TaskIdKey to task.id))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${steps.count { it.isDone }}/${steps.size} ${if (expanded) "▾" else "▸"}",
                    style = TextStyle(color = TextDim, fontSize = 12.sp),
                )
            }
        }
    }
    if (expanded) steps.forEach { StepRow(it) }
    }
}

@Composable
private fun StepRow(step: Subtask) {
    Row(GlanceModifier.fillMaxWidth().padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        // Larger tap target than the visible 16 dp circle.
        Box(
            GlanceModifier
                .size(36.dp)
                .clickable(actionRunCallback<ToggleStepAction>(actionParametersOf(StepIdKey to step.id))),
            contentAlignment = Alignment.Center,
        ) {
            if (step.isDone) {
                Box(
                    GlanceModifier.size(16.dp).background(ImageProvider(R.drawable.widget_check_done_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✓",
                        style = TextStyle(color = ColorProvider(Color(0xFF152C52)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Box(GlanceModifier.size(16.dp).background(ImageProvider(R.drawable.widget_check_background))) {}
            }
        }
        Spacer(GlanceModifier.width(4.dp))
        Text(
            step.title,
            style = TextStyle(color = if (step.isDone) TextDim else TextWhite, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}
