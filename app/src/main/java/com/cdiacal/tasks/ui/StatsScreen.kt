package com.cdiacal.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdiacal.tasks.R
import com.cdiacal.tasks.ui.components.CabeceraHero
import com.cdiacal.tasks.ui.components.CifraHero
import com.cdiacal.tasks.ui.components.MiniDatoHero
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(), onOpenSettings: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Week labels are the Monday of each week, "d/M".
    val labels = remember(state.today) {
        List(STATS_WEEKS) { i ->
            val monday = LocalDate.ofEpochDay(weekStart(state.today) - 7L * (STATS_WEEKS - 1 - i))
            "${monday.dayOfMonth}/${monday.monthValue}"
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StatsHero(state, onOpenSettings)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SeccionTarjeta(stringResource(R.string.stats_tasks_by_week)) {
                BarChart(
                    values = state.taskWeeks.map { it.toFloat() },
                    labels = labels,
                    valueText = { it.toInt().toString() },
                    maxValue = maxOf(1, state.taskWeeks.max()).toFloat(),
                )
                if (state.completedTotal == 0) {
                    Text(
                        stringResource(R.string.stats_tasks_empty),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Fact(stringResource(R.string.stats_pending), state.pending.toString(), Modifier.weight(1f))
                    Fact(stringResource(R.string.stats_overdue), state.overdue.toString(), Modifier.weight(1f))
                }
            }
            SeccionTarjeta(stringResource(R.string.stats_habits_by_week)) {
                BarChart(
                    values = state.habitWeeks.map { it?.toFloat() },
                    labels = labels,
                    valueText = { "${it.toInt()}%" },
                    maxValue = 100f,
                )
                Text(
                    stringResource(R.string.stats_habits_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.habitRows.isNotEmpty()) {
                SeccionTarjeta(stringResource(R.string.stats_streaks)) {
                    state.habitRows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                listOf(row.habit.emoji, row.habit.name).filter { it.isNotBlank() }.joinToString(" "),
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                maxLines = 1,
                            )
                            Text(
                                stringResource(R.string.stats_streak_row, row.current, row.best),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHero(state: StatsState, onSettings: () -> Unit) {
    val today = remember { DateFormat.getDateInstance(DateFormat.LONG).format(Date()) }
    CabeceraHero {
        Column(Modifier.padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.stats_title),
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
                stringResource(R.string.stats_hero_label),
                modifier = Modifier.padding(top = 18.dp),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.5.sp,
            )
            CifraHero(state.completedThisWeek.toString())
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniDatoHero(stringResource(R.string.stats_last_30), state.completedLast30.toString(), Modifier.weight(1f))
                MiniDatoHero(stringResource(R.string.stats_total), state.completedTotal.toString(), Modifier.weight(1f))
                MiniDatoHero(
                    stringResource(R.string.stats_habits_30),
                    state.habitRate30?.let { "$it%" } ?: "–",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Plain vertical bars, one per entry, the last one (the running week) at full strength. Every
 * bar shows its value as text above it, so nothing depends on colour or on reading an axis.
 * A null value means "no data that week".
 */
@Composable
private fun BarChart(
    values: List<Float?>,
    labels: List<String>,
    valueText: (Float) -> String,
    maxValue: Float,
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEachIndexed { i, value ->
            val last = i == values.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value?.let(valueText) ?: "–",
                    fontSize = 11.sp,
                    fontWeight = if (last) FontWeight.Bold else FontWeight.Normal,
                    color = if (last) MaterialTheme.colorScheme.onSurface else muted,
                    maxLines = 1,
                )
                Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (value != null) {
                        val fraction = (value / maxValue).coerceIn(0f, 1f).coerceAtLeast(0.02f)
                        Column(
                            Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (value <= 0f) MaterialTheme.colorScheme.outline else if (last) primary else primary.copy(alpha = 0.45f)),
                        ) {}
                    }
                }
                Text(
                    labels.getOrElse(i) { "" },
                    fontSize = 10.sp,
                    fontWeight = if (last) FontWeight.Bold else FontWeight.Normal,
                    color = if (last) MaterialTheme.colorScheme.onSurface else muted,
                    maxLines = 1,
                )
            }
        }
    }
}
