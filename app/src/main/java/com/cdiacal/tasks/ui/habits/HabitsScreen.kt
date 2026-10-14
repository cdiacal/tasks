package com.cdiacal.tasks.ui.habits

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdiacal.tasks.R
import com.cdiacal.tasks.habits.DayCell
import com.cdiacal.tasks.habits.SlotCell
import com.cdiacal.tasks.habits.slotEmoji
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitItem
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.habits.formatMinutes
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.CabeceraHero
import com.cdiacal.tasks.ui.components.CifraHero
import com.cdiacal.tasks.ui.components.IconoInicial
import com.cdiacal.tasks.ui.components.MiniDatoHero
import com.cdiacal.tasks.ui.components.TarjetaPanel
import com.cdiacal.tasks.ui.theme.colorIngreso
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
    newHabitTrigger: Int = 0,
    onNewHabitConsumed: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // null = dialog closed; Habit(name = "") = new habit; otherwise editing.
    var editing by remember { mutableStateOf<Habit?>(null) }
    // The launcher shortcut «Nuevo hábito» opens the editor straight away.
    LaunchedEffect(newHabitTrigger) {
        if (newHabitTrigger > 0) {
            editing = Habit(name = "")
            onNewHabitConsumed()
        }
    }
    // Habit whose history calendar is open; looked up live so toggling updates it instantly.
    var detailId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* If denied, reminders are saved but no notification is shown. */ }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            BotonPrincipal(
                texto = stringResource(R.string.new_habit),
                icono = Icons.Default.Add,
                onClick = { editing = Habit(name = "") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            HabitsHero(state, onOpenSettings)
            if (state.items.isEmpty()) {
                EmptyHabits()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 128.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.habit.id }) { item ->
                        HabitCard(
                            item = item,
                            onToggle = { day -> viewModel.toggle(item.habit.id, day) },
                            onToggleSlot = { slot -> viewModel.toggleSlot(item.habit.id, state.today, slot) },
                            onClick = { detailId = item.habit.id },
                        )
                    }
                }
            }
        }
    }

    val detailItem = detailId?.let { id -> state.items.firstOrNull { it.habit.id == id } }
    if (detailItem != null) {
        HabitDetailDialog(
            item = detailItem,
            today = state.today,
            onToggle = { day -> viewModel.toggle(detailItem.habit.id, day) },
            onEdit = {
                detailId = null
                editing = detailItem.habit
            },
            onDismiss = { detailId = null },
        )
    }

    editing?.let { habit ->
        HabitEditorDialog(
            initial = habit,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (it.reminderMinutes != null && needsPermission) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDelete = {
                viewModel.delete(habit)
                editing = null
            },
        )
    }
}

@Composable
private fun HabitsHero(state: HabitsState, onSettings: () -> Unit) {
    val today = remember { DateFormat.getDateInstance(DateFormat.LONG).format(Date()) }
    val context = LocalContext.current
    CabeceraHero {
        Column(Modifier.padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.habits_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(today, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color.White)
                }
            }
            Text(
                stringResource(R.string.habits_today_label),
                modifier = Modifier.padding(top = 18.dp),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.5.sp,
            )
            CifraHero("${state.todaySlotsDone}/${state.todaySlotsTotal}")
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniDatoHero(
                    stringResource(R.string.habit_stat_streak),
                    context.resources.getQuantityString(R.plurals.habit_days, state.bestCurrentStreak, state.bestCurrentStreak),
                    Modifier.weight(1f),
                )
                MiniDatoHero(stringResource(R.string.habit_stat_week), "${state.weekPercent}%", Modifier.weight(1f))
                MiniDatoHero(stringResource(R.string.habit_stat_total), state.items.size.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyHabits() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.habits_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitCard(item: HabitItem, onToggle: (Long) -> Unit, onToggleSlot: (Int) -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val streak = item.streaks
    fun days(n: Int) = context.resources.getQuantityString(R.plurals.habit_days, n, n)
    val subtitle = when {
        streak.current > 0 -> stringResource(R.string.habit_streak_current, days(streak.current), days(streak.best))
        streak.best > 0 -> stringResource(R.string.habit_streak_best, days(streak.best))
        else -> stringResource(R.string.habit_no_streak)
    }
    val todayCell = item.week.last()

    TarjetaPanel(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconoInicial(
                    inicial = item.habit.name.take(1).uppercase(),
                    id = item.habit.id,
                    emoji = item.habit.emoji,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.habit.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(subtitle, fontSize = 12.sp, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.habit.reminderMinutes?.let {
                        Text("⏰ ${formatMinutes(it)}", fontSize = 12.sp, color = secondary, maxLines = 1)
                    }
                }
                if (item.slots.isEmpty()) {
                    DayCheck(cell = todayCell, size = 36.dp, onClick = { onToggle(todayCell.epochDay) })
                } else {
                    // A habit done several times a day: one tick per part of the day.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.slots.forEach { cell ->
                            SlotCheck(cell, enabled = todayCell.scheduled, onClick = { onToggleSlot(cell.slot.value) })
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                item.week.forEach { cell ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            dayInitial(cell.epochDay),
                            fontSize = 10.5.sp,
                            fontWeight = if (cell.isToday) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (cell.isToday) MaterialTheme.colorScheme.primary else secondary,
                        )
                        DayCheck(cell = cell, size = 24.dp, onClick = { onToggle(cell.epochDay) })
                    }
                }
            }
        }
    }
}

/** One part of today (morning / noon / night): its emoji, green behind it when done. */
@Composable
private fun SlotCheck(cell: SlotCell, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(slotLabelRes(cell.slot))
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (cell.done) Modifier.background(colorIngreso())
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape),
            )
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(slotEmoji(cell.slot), fontSize = 16.sp)
    }
}

private fun dayInitial(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase()

/**
 * One day of a habit: green when done, outlined when open, a small dot when the habit is not
 * scheduled that day (not tappable). Today gets a primary ring.
 */
@Composable
private fun DayCheck(cell: DayCell, size: Dp, onClick: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val ring = when {
        cell.isToday && !cell.done -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        cell.partial -> Modifier.border(1.5.dp, colorIngreso(), CircleShape)
        !cell.done && cell.scheduled -> Modifier.border(1.5.dp, secondary.copy(alpha = 0.5f), CircleShape)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (cell.done) Modifier.background(colorIngreso()) else if (cell.partial) Modifier.background(colorIngreso().copy(alpha = 0.3f)) else Modifier)
            .then(ring)
            .clickable(enabled = cell.scheduled, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.done -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(size * 0.6f),
            )
            !cell.scheduled -> Box(
                Modifier.size(size / 4).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
