package com.cdiacal.tasks.calendar

import com.cdiacal.tasks.data.Task
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** What an event looks like it needs: a gift, a plain to-do, some preparation, or nothing. */
enum class SuggestionKind { GIFT, TASK, PREPARE, EVENT }

/**
 * The task proposed for an event. [preselected] is the app's guess that this event is something
 * to do (rather than just something that happens); the user always has the last word.
 */
data class TaskSuggestion(
    val kind: SuggestionKind,
    val title: String,
    val due: LocalDate,
    val preselected: Boolean,
)

private const val GIFT_DAYS_BEFORE = 3L
private const val PREPARE_DAYS_BEFORE = 1L

private val GIFT = Regex("\\b(cumple|cumpleanos|aniversario|boda|bautizo|comunion|onomastica|birthday|anniversary)\\b")
private val TASK_PREFIX = Regex("^(recordatorio|recordar|tarea|todo|to-do|pendiente)\\b[\\s:\\-–]*")
private val TASK_VERB = Regex(
    "^(comprar|pagar|entregar|enviar|mandar|llamar|renovar|recoger|devolver|reservar|revisar|pedir|cancelar|" +
        "llevar|terminar|solicitar|imprimir|regar|limpiar|hacer|firmar|presentar|preparar)\\b",
)
private val TASK_WORD = Regex("\\b(fecha limite|deadline|vencimiento|vence|plazo)\\b")
private val PREPARE = Regex("\\b(examen|entrevista|presentacion|exposicion|defensa|oposicion|ponencia|charla)\\b")

/** Lower case without accents, so "Cumpleaños" and "cumpleanos" are the same word. */
private fun fold(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/** The event's first day inside the window, i.e. today for an event already running. */
fun eventStartDay(event: CalendarEvent, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    maxOf(daysCovered(event, zone).first, today)

fun suggestTask(event: CalendarEvent, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TaskSuggestion {
    val title = event.title.trim()
    val day = eventStartDay(event, today, zone)
    if (title.isEmpty()) return TaskSuggestion(SuggestionKind.EVENT, "Evento del calendario", day, false)
    val folded = fold(title)

    if (GIFT.containsMatchIn(folded)) {
        return TaskSuggestion(SuggestionKind.GIFT, "Comprar regalo: $title", maxOf(day.minusDays(GIFT_DAYS_BEFORE), today), true)
    }
    val prefix = TASK_PREFIX.find(folded)
    if (prefix != null) {
        // Folding keeps the length for ordinary text, so the prefix can be cut from the original.
        val rest = if (folded.length == title.length) title.substring(prefix.range.last + 1).trim() else title
        return TaskSuggestion(SuggestionKind.TASK, rest.ifEmpty { title }, day, true)
    }
    if (TASK_VERB.containsMatchIn(folded) || TASK_WORD.containsMatchIn(folded)) {
        return TaskSuggestion(SuggestionKind.TASK, title, day, true)
    }
    if (PREPARE.containsMatchIn(folded)) {
        return TaskSuggestion(SuggestionKind.PREPARE, "Preparar: $title", maxOf(day.minusDays(PREPARE_DAYS_BEFORE), today), true)
    }
    return TaskSuggestion(SuggestionKind.EVENT, title, day, false)
}

/** Identity of one occurrence of an event, used to remember what was already imported. */
fun eventKey(event: CalendarEvent): String = "${event.eventId}:${event.beginMillis}"

/** Imported-event keys older than [cutoffMillis] are forgotten so the set never grows forever. */
fun pruneImported(keys: Set<String>, cutoffMillis: Long): Set<String> =
    keys.filterTo(mutableSetOf()) { key -> key.substringAfter(':').toLongOrNull()?.let { it >= cutoffMillis } ?: false }

/** The task to create for [event] following [suggestion]; due dates are UTC midnights like everywhere else. */
fun suggestionToTask(event: CalendarEvent, suggestion: TaskSuggestion): Task = Task(
    title = suggestion.title,
    notes = "Del calendario (${event.calendarName}): ${event.title.trim()}".trim(),
    dueDate = suggestion.due.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
)
