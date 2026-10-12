package com.cdiacal.tasks.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.calendar.CalendarEvent
import com.cdiacal.tasks.calendar.CalendarPrefs
import com.cdiacal.tasks.calendar.CalendarReader
import com.cdiacal.tasks.calendar.SuggestionKind
import com.cdiacal.tasks.calendar.TaskSuggestion
import com.cdiacal.tasks.calendar.eventKey
import com.cdiacal.tasks.calendar.groupByDay
import com.cdiacal.tasks.calendar.suggestTask
import com.cdiacal.tasks.calendar.suggestionToTask
import com.cdiacal.tasks.ui.components.BotonPrincipal
import com.cdiacal.tasks.ui.components.CabeceraSimple
import com.cdiacal.tasks.ui.components.Pastilla
import com.cdiacal.tasks.ui.components.SeccionTarjeta
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlinx.coroutines.launch

private const val PREVIEW_DAYS = 30

/**
 * The next 30 days of the phone's calendars with a preview of the tasks they would become.
 * Events that look like something to do come pre-ticked; the user ticks or unticks freely and
 * imports. Nothing is ever written back to the calendar.
 */
@Composable
fun CalendarPreviewScreen(viewModel: TasksViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var granted by remember { mutableStateOf(CalendarReader.hasPermission(context)) }
    var denied by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }
    // Names of calendars switched off; remembered between visits.
    var hidden by remember { mutableStateOf(CalendarPrefs.hidden(context)) }
    var imported by remember { mutableStateOf(CalendarPrefs.imported(context)) }
    val today = remember { LocalDate.now() }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(granted) {
        events = if (granted) CalendarReader.upcoming(context, PREVIEW_DAYS) else null
    }

    val suggestions = remember(events) {
        events.orEmpty().associate { eventKey(it) to suggestTask(it, today) }
    }
    var selected by remember(events) {
        mutableStateOf(
            events.orEmpty()
                .filter { suggestions[eventKey(it)]?.preselected == true && eventKey(it) !in imported }
                .map { eventKey(it) }
                .toSet(),
        )
    }
    val visibleEvents = remember(events, hidden) { events.orEmpty().filter { it.calendarName !in hidden } }
    val toImport = remember(visibleEvents, selected) {
        visibleEvents.filter { eventKey(it) in selected && eventKey(it) !in imported }.distinctBy { eventKey(it) }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
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
                    else -> EventsList(
                        events = list,
                        hidden = hidden,
                        today = today,
                        suggestions = suggestions,
                        selected = selected,
                        imported = imported,
                        onToggleCalendar = { name ->
                            hidden = if (name in hidden) hidden - name else hidden + name
                            CalendarPrefs.setHidden(context, hidden)
                        },
                        onToggleEvent = { key -> selected = if (key in selected) selected - key else selected + key },
                        onSelectSuggested = {
                            selected = visibleEvents
                                .filter { suggestions[eventKey(it)]?.preselected == true && eventKey(it) !in imported }
                                .map { eventKey(it) }
                                .toSet()
                        },
                        onSelectNone = { selected = emptySet() },
                    )
                }
            }
            if (granted && events != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    BotonPrincipal(
                        texto = if (toImport.isEmpty()) {
                            stringResource(R.string.calendar_import_none)
                        } else {
                            stringResource(R.string.calendar_import_button, toImport.size)
                        },
                        enabled = toImport.isNotEmpty(),
                        onClick = {
                            val tasks = toImport.map { suggestionToTask(it, suggestions.getValue(eventKey(it))) }
                            val keys = toImport.map { eventKey(it) }.toSet()
                            viewModel.importTasks(tasks)
                            CalendarPrefs.addImported(context, keys)
                            imported = imported + keys
                            selected = selected - keys
                            val message = context.getString(R.string.calendar_imported_n, tasks.size)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(message)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventsList(
    events: List<CalendarEvent>,
    hidden: Set<String>,
    today: LocalDate,
    suggestions: Map<String, TaskSuggestion>,
    selected: Set<String>,
    imported: Set<String>,
    onToggleCalendar: (String) -> Unit,
    onToggleEvent: (String) -> Unit,
    onSelectSuggested: () -> Unit,
    onSelectNone: () -> Unit,
) {
    val calendars = remember(events) { events.map { it.calendarName }.distinct().sorted() }
    if (calendars.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            calendars.forEach { name ->
                Pastilla(texto = name, activa = name !in hidden, onClick = { onToggleCalendar(name) })
            }
        }
    }
    val grouped = remember(events, hidden) {
        groupByDay(events.filter { it.calendarName !in hidden }, today, PREVIEW_DAYS)
    }
    if (grouped.isEmpty()) {
        Text(stringResource(R.string.calendar_empty), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.calendar_count, grouped.values.flatten().distinctBy { eventKey(it) }.size),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.calendar_select_suggested),
            modifier = Modifier.clickable(onClick = onSelectSuggested),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.calendar_select_none),
            modifier = Modifier.clickable(onClick = onSelectNone),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    val dayFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val shortDay = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    grouped.forEach { (day, dayEvents) ->
        SeccionTarjeta(dayFormat.format(day)) {
            dayEvents.forEach { event ->
                val key = eventKey(event)
                val done = key in imported
                val suggestion = suggestions[key]
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !done) { onToggleEvent(key) },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(checked = key in selected && !done, enabled = !done, onCheckedChange = { onToggleEvent(key) })
                    Column(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (event.allDay) stringResource(R.string.calendar_all_day) else timeFormat.format(Date(event.beginMillis)),
                                modifier = Modifier.width(72.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(event.title.ifBlank { stringResource(R.string.calendar_untitled) }, fontSize = 14.sp)
                        }
                        if (done) {
                            Text(stringResource(R.string.calendar_already_imported), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (suggestion != null) {
                            Text(
                                suggestionLabel(suggestion, shortDay.format(suggestion.due)),
                                fontSize = 12.sp,
                                color = if (suggestion.preselected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (calendars.size > 1) {
                            Text(event.calendarName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun suggestionLabel(suggestion: TaskSuggestion, date: String): String = stringResource(
    when (suggestion.kind) {
        SuggestionKind.GIFT -> R.string.calendar_suggest_gift
        SuggestionKind.TASK -> R.string.calendar_suggest_task
        SuggestionKind.PREPARE -> R.string.calendar_suggest_prepare
        SuggestionKind.EVENT -> R.string.calendar_suggest_event
    },
    suggestion.title,
    date,
)
