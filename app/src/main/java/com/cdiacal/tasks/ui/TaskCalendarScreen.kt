package com.cdiacal.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.priorityKind
import com.cdiacal.tasks.ui.components.CabeceraHero
import com.cdiacal.tasks.ui.components.CifraHero
import com.cdiacal.tasks.ui.components.MiniDatoHero
import com.cdiacal.tasks.ui.components.PuntoColor
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import com.cdiacal.tasks.ui.theme.colorGasto
import com.cdiacal.tasks.ui.theme.colorIngreso
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** A month grid of the tasks by due date, with the tasks of the tapped day listed underneath. */
@Composable
fun TaskCalendarScreen(viewModel: TasksViewModel, onOpenSettings: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember(state.todayUtcMillis) { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    val weeks = remember(month, state.allTasks, today) { buildTaskMonth(month, state.allTasks, today) }
    val dayTasks = remember(selected, state.allTasks) { tasksOn(selected, state.allTasks) }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CabeceraHero {
            Column(Modifier.padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.calendar_tab_title),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.calendar_tab_subtitle),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color.White)
                    }
                }
                val overdue = state.stats.overdue
                Text(
                    stringResource(R.string.stats_pending),
                    modifier = Modifier.padding(top = 18.dp),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.5.sp,
                )
                CifraHero(state.stats.pending.toString())
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniDatoHero(stringResource(R.string.stats_overdue), overdue.toString(), Modifier.weight(1f))
                    MiniDatoHero(stringResource(R.string.stat_today), state.stats.dueToday.toString(), Modifier.weight(1f))
                    MiniDatoHero(stringResource(R.string.stat_done), state.stats.done.toString(), Modifier.weight(1f))
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SeccionTarjeta(null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        monthTitleOf(month),
                        modifier = Modifier.weight(1f).clickable {
                            month = YearMonth.from(today)
                            selected = today
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    for (i in 1..7) {
                        Text(
                            DayOfWeek.of(i).getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.5.sp,
                            color = secondary,
                        )
                    }
                }
                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            DayCell(
                                day = day,
                                isToday = day.date == today,
                                isSelected = day.date == selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selected = day.date
                                    if (!day.inMonth) month = YearMonth.from(day.date)
                                },
                            )
                        }
                    }
                }
            }
            SeccionTarjeta(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(selected)) {
                if (dayTasks.isEmpty()) {
                    Text(stringResource(R.string.calendar_tab_empty_day), fontSize = 13.sp, color = secondary)
                } else {
                    dayTasks.forEach { task -> DayTaskRow(task, onToggle = { viewModel.toggleDone(task) }) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, isToday: Boolean, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.aspectRatio(0.85f).padding(2.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .then(if (isSelected) Modifier.background(primary.copy(alpha = 0.16f)) else Modifier)
                .then(if (isToday) Modifier.border(1.5.dp, primary, RoundedCornerShape(10.dp)) else Modifier)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (day.inMonth) MaterialTheme.colorScheme.onSurface else secondary.copy(alpha = 0.5f),
            )
            Box(Modifier.padding(top = 3.dp).height(5.dp)) {
                if (day.pending > 0) {
                    val dot = when {
                        day.overdue -> colorGasto()
                        else -> priorityColor(day.topPriority) ?: primary
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(minOf(day.pending, 3)) { PuntoColor(dot, tamano = 5.dp) }
                    }
                } else if (day.done > 0) {
                    PuntoColor(colorIngreso().copy(alpha = 0.6f), tamano = 5.dp)
                }
            }
        }
    }
}

@Composable
private fun DayTaskRow(task: Task, onToggle: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dot = if (task.isDone) null else priorityColor(task.priorityKind())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = colorIngreso(),
                checkmarkColor = MaterialTheme.colorScheme.background,
                uncheckedColor = secondary,
            ),
        )
        if (dot != null) PuntoColor(dot)
        Text(
            task.title,
            modifier = Modifier.weight(1f).padding(start = if (dot != null) 4.dp else 0.dp),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            color = if (task.isDone) secondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun monthTitleOf(month: YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    return "${name.replaceFirstChar { it.uppercase() }} ${month.year}"
}
