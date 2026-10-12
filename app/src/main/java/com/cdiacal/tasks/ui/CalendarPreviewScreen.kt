package com.cdiacal.tasks.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.calendar.CalendarEvent
import com.cdiacal.tasks.calendar.CalendarReader
import com.cdiacal.tasks.calendar.groupByDay
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.CabeceraSimple
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

private const val PREVIEW_DAYS = 30

/**
 * Preview of what is in the phone's calendars for the next 30 days. Read-only: nothing is
 * imported or changed yet, it only shows what could become tasks.
 */
@Composable
fun CalendarPreviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(CalendarReader.hasPermission(context)) }
    var denied by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }
    // Names of calendars the user switched off in this preview.
    var hidden by remember { mutableStateOf(setOf<String>()) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(granted) {
        events = if (granted) CalendarReader.upcoming(context, PREVIEW_DAYS) else null
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CabeceraSimple(
                titulo = stringResource(R.string.calendar_title),
                subtitulo = stringResource(R.string.calendar_subtitle, PREVIEW_DAYS),
                onBack = onBack,
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.calendar_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val list = events
                when {
                    !granted -> SeccionTarjeta(null) {
                        Text(stringResource(R.string.calendar_permission_text), fontSize = 14.sp)
                        if (denied) {
                            Text(
                                stringResource(R.string.calendar_permission_denied),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BotonPrincipal(
                            texto = stringResource(R.string.calendar_allow),
                            onClick = { permission.launch(Manifest.permission.READ_CALENDAR) },
                        )
                    }
                    list == null -> Text(stringResource(R.string.calendar_loading), fontSize = 14.sp)
                    else -> EventsList(list, hidden) { name ->
                        hidden = if (name in hidden) hidden - name else hidden + name
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsList(events: List<CalendarEvent>, hidden: Set<String>, onToggleCalendar: (String) -> Unit) {
    val calendars = remember(events) { events.map { it.calendarName }.distinct().sorted() }
    if (calendars.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            calendars.forEach { name ->
                Pastilla(texto = name, activa = name !in hidden, onClick = { onToggleCalendar(name) })
            }
        }
    }
    val today = remember { LocalDate.now() }
    val grouped = remember(events, hidden) {
        groupByDay(events.filter { it.calendarName !in hidden }, today, PREVIEW_DAYS)
    }
    if (grouped.isEmpty()) {
        Text(stringResource(R.string.calendar_empty), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Text(
        stringResource(R.string.calendar_count, grouped.values.flatten().distinctBy { it.eventId to it.beginMillis }.size),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val dayFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    grouped.forEach { (day, dayEvents) ->
        SeccionTarjeta(dayFormat.format(day)) {
            dayEvents.forEach { event ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (event.allDay) stringResource(R.string.calendar_all_day) else timeFormat.format(Date(event.beginMillis)),
                        modifier = Modifier.width(76.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(event.title.ifBlank { stringResource(R.string.calendar_untitled) }, fontSize = 14.sp)
                        if (calendars.size > 1) {
                            Text(event.calendarName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
