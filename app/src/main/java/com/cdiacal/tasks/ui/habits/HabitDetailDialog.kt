package com.cdiacal.tasks.ui.habits

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cdiacal.tasks.R
import com.cdiacal.tasks.habits.HabitItem
import com.cdiacal.tasks.habits.MonthDay
import com.cdiacal.tasks.habits.buildMonthGrid
import com.cdiacal.tasks.habits.historyStart
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.BotonSecundario
import com.cdiacal.tasks.ui.components.IconoInicial
import com.cdiacal.tasks.ui.theme.colorIngreso
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** A habit's full history: key figures plus a month calendar whose past days can be toggled. */
@Composable
fun HabitDetailDialog(
    item: HabitItem,
    today: Long,
    onToggle: (Long) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val currentMonth = remember(today) { YearMonth.from(LocalDate.ofEpochDay(today)) }
    val firstMonth = remember(item.startDay, item.doneDays) {
        YearMonth.from(LocalDate.ofEpochDay(historyStart(item.startDay, item.doneDays)))
    }
    var month by remember(item.habit.id) { mutableStateOf(currentMonth) }
    val grid = remember(month, item.habit.daysMask, item.doneDays, item.startDay, today) {
        buildMonthGrid(month, item.habit.daysMask, item.doneDays, item.startDay, today)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconoInicial(
                        inicial = item.habit.name.take(1).uppercase(),
                        id = item.habit.id,
                        emoji = item.habit.emoji,
                    )
                    Text(
                        item.habit.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Figure(stringResource(R.string.stat_current_streak), item.streaks.current.toString(), Modifier.weight(1f))
                    Figure(stringResource(R.string.stat_best_streak), item.streaks.best.toString(), Modifier.weight(1f))
                    Figure(stringResource(R.string.stat_total_done), item.doneDays.size.toString(), Modifier.weight(1f))
                }

                // Month navigation, bounded by the habit's first day and the current month.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > firstMonth) {
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        monthTitle(month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < currentMonth) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    val cells: List<MonthDay?> = List(grid.leadingBlanks) { null } + grid.days
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (day != null) DayCircle(day, onClick = { onToggle(day.epochDay) })
                                }
                            }
                            // Pad the last week so every row keeps the same column widths.
                            repeat(7 - week.size) { Box(Modifier.weight(1f).aspectRatio(1f)) }
                        }
                    }
                }

                Text(
                    if (grid.scheduledSoFar == 0) {
                        stringResource(R.string.habit_month_empty)
                    } else {
                        stringResource(R.string.habit_month_summary, grid.completed, grid.scheduledSoFar, grid.percent)
                    },
                    fontSize = 13.sp,
                    color = secondary,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    BotonSecundario(texto = stringResource(R.string.habit_detail_edit), onClick = onEdit)
                    BotonPrincipal(texto = stringResource(R.string.close), onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * Done = green; missed past scheduled day = muted fill; today = primary ring; unscheduled = faded.
 * Only scheduled days that are not in the future can be toggled.
 */
@Composable
private fun DayCircle(day: MonthDay, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val missed = day.scheduled && !day.isFuture && !day.done
    val textColor: Color = when {
        day.done -> colors.background
        day.isToday -> colors.primary
        !day.scheduled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        else -> colors.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .then(
                when {
                    day.done -> Modifier.background(colorIngreso())
                    missed -> Modifier.background(colors.surfaceVariant)
                    else -> Modifier
                },
            )
            .then(if (day.isToday && !day.done) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clickable(enabled = day.scheduled && !day.isFuture, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (day.done || day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

private fun monthTitle(month: YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    return "${name.replaceFirstChar { it.uppercase() }} ${month.year}"
}
