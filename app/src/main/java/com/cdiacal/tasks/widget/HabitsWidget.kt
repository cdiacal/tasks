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
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.habits.HabitItem
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.habits.buildHabitsState
import com.cdiacal.tasks.habits.slotEmoji
import java.time.LocalDate

private val HabitIdKey = ActionParameters.Key<Long>("habit_id")
private val SlotKey = ActionParameters.Key<Int>("slot")

/** Today's habits with a one-tap check. Same fixed widget palette as the tasks widget. */
class HabitsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = TaskDatabase.get(context).habitDao()
        // Read once so the first frame is correct, then observe the database so ticking a habit
        // (here, in the app or from a notification) redraws the widget by itself.
        val initialHabits = dao.getAllHabits().sortedWith(compareBy({ it.createdAt }, { it.id }))
        val initialChecks = dao.getAllChecks()
        provideContent {
            val habits by dao.observeHabits().collectAsState(initialHabits)
            val checks by dao.observeChecks().collectAsState(initialChecks)
            val today = LocalDate.now().toEpochDay()
            val state = remember(habits, checks, today) { buildHabitsState(habits, checks, today) }
            HabitsWidgetContent(state)
        }
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

/** Toggles today's check for a habit from the widget (refreshes the widget and the app). */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        createHabitRepository(context).toggle(id, LocalDate.now().toEpochDay())
    }
}

/** Ticks / unticks one part of today (morning / noon / night) of a habit from the widget. */
class ToggleHabitSlotAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        val slot = parameters[SlotKey] ?: return
        createHabitRepository(context).toggleSlot(id, LocalDate.now().toEpochDay(), slot)
    }
}

@Composable
private fun HabitsWidgetContent(state: HabitsState) {
    val context = LocalContext.current
    val openHabits = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_HABITS, true)
    // Open habits first; done ones sink to the bottom (sortedBy is stable).
    val due = state.items.filter { it.scheduledToday }.sortedBy { it.doneToday }
    val allDone = state.todaySlotsTotal > 0 && state.todaySlotsDone == state.todaySlotsTotal

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp),
    ) {
        Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).clickable(actionStartActivity(openHabits))) {
            Text(
                context.getString(R.string.tab_habits),
                style = TextStyle(color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                if (state.todayTotal == 0) {
                    context.getString(R.string.widget_habits_nothing)
                } else {
                    context.getString(R.string.widget_habits_progress, state.todaySlotsDone, state.todaySlotsTotal)
                },
                style = TextStyle(color = if (allDone) Green else TextDim, fontSize = 12.sp),
            )
        }

        when {
            state.items.isEmpty() -> Message(context.getString(R.string.widget_habits_empty), openHabits)
            due.isEmpty() -> Message(context.getString(R.string.widget_habits_none_today), openHabits)
            else -> LazyColumn(GlanceModifier.fillMaxSize()) {
                items(due, itemId = { it.habit.id }) { item -> HabitRow(item) }
            }
        }
    }
}

@Composable
private fun Message(text: String, open: Intent) {
    Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = TextDim, fontSize = 13.sp))
    }
}

@Composable
private fun HabitRow(item: HabitItem) {
    val title = listOf(item.habit.emoji, item.habit.name).filter { it.isNotBlank() }.joinToString(" ")
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (item.slots.isEmpty()) {
            // Larger tap target than the visible 20 dp circle.
            Box(
                GlanceModifier
                    .size(40.dp)
                    .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to item.habit.id))),
                contentAlignment = Alignment.Center,
            ) {
                if (item.doneToday) {
                    Box(
                        GlanceModifier.size(20.dp).background(ImageProvider(R.drawable.widget_check_done_background)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", style = TextStyle(color = ColorProvider(Color(0xFF152C52)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                } else {
                    Box(GlanceModifier.size(20.dp).background(ImageProvider(R.drawable.widget_check_background))) {}
                }
            }
            Spacer(GlanceModifier.width(4.dp))
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(
                title,
                style = TextStyle(color = if (item.doneToday) TextDim else TextWhite, fontSize = 14.sp),
                maxLines = 1,
            )
            if (item.streaks.current > 0) {
                Text("🔥 ${item.streaks.current}", style = TextStyle(color = TextDim, fontSize = 11.sp))
            }
        }
        // A habit done several times a day: one tap target per part of the day.
        item.slots.forEach { cell ->
            Box(
                GlanceModifier
                    .size(38.dp)
                    .clickable(
                        actionRunCallback<ToggleHabitSlotAction>(
                            actionParametersOf(HabitIdKey to item.habit.id, SlotKey to cell.slot.value),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    GlanceModifier
                        .size(30.dp)
                        .background(ImageProvider(if (cell.done) R.drawable.widget_check_done_background else R.drawable.widget_check_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(slotEmoji(cell.slot), style = TextStyle(fontSize = 14.sp))
                }
            }
        }
    }
}
