package com.cdiacal.tasks

import com.cdiacal.tasks.calendar.CalendarEvent
import com.cdiacal.tasks.calendar.SuggestionKind
import com.cdiacal.tasks.calendar.eventKey
import com.cdiacal.tasks.calendar.pruneImported
import com.cdiacal.tasks.calendar.suggestTask
import com.cdiacal.tasks.calendar.suggestionToTask
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSuggestionsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2024, 5, 15)

    private fun event(title: String, day: LocalDate = today.plusDays(10), id: Long = 1) = CalendarEvent(
        eventId = id,
        title = title,
        beginMillis = day.atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli(),
        endMillis = day.atStartOfDay(zone).plusHours(10).toInstant().toEpochMilli(),
        allDay = false,
        calendarName = "Personal",
    )

    @Test fun birthdayBecomesAGiftTaskThreeDaysBefore() {
        val s = suggestTask(event("Cumpleaños de Ana"), today, zone)
        assertEquals(SuggestionKind.GIFT, s.kind)
        assertEquals("Comprar regalo: Cumpleaños de Ana", s.title)
        assertEquals(today.plusDays(7), s.due)
        assertTrue(s.preselected)
    }

    @Test fun giftDueDateNeverFallsBeforeToday() {
        val s = suggestTask(event("Cumple de Luis", today.plusDays(1)), today, zone)
        assertEquals(today, s.due)
    }

    @Test fun englishBirthdayAndAccentsAreRecognised() {
        assertEquals(SuggestionKind.GIFT, suggestTask(event("Marta's birthday"), today, zone).kind)
        assertEquals(SuggestionKind.GIFT, suggestTask(event("ANIVERSARIO de boda"), today, zone).kind)
    }

    @Test fun reminderPrefixIsStrippedAndKeptAsTask() {
        val s = suggestTask(event("Recordatorio: renovar el DNI"), today, zone)
        assertEquals(SuggestionKind.TASK, s.kind)
        assertEquals("renovar el DNI", s.title)
        assertEquals(today.plusDays(10), s.due)
    }

    @Test fun actionVerbAtTheStartIsATask() {
        val s = suggestTask(event("Pagar alquiler"), today, zone)
        assertEquals(SuggestionKind.TASK, s.kind)
        assertEquals("Pagar alquiler", s.title)
    }

    @Test fun deadlineWordsMakeATask() {
        assertEquals(SuggestionKind.TASK, suggestTask(event("Fecha límite informe"), today, zone).kind)
    }

    @Test fun examNeedsPreparationTheDayBefore() {
        val s = suggestTask(event("Examen de inglés"), today, zone)
        assertEquals(SuggestionKind.PREPARE, s.kind)
        assertEquals("Preparar: Examen de inglés", s.title)
        assertEquals(today.plusDays(9), s.due)
    }

    @Test fun plainMeetingIsJustAnEventNotPreselected() {
        val s = suggestTask(event("Reunión de equipo"), today, zone)
        assertEquals(SuggestionKind.EVENT, s.kind)
        assertFalse(s.preselected)
    }

    @Test fun cumplimientoIsNotABirthday() {
        assertEquals(SuggestionKind.EVENT, suggestTask(event("Revisión de cumplimiento"), today, zone).kind)
    }

    @Test fun blankTitleIsAnEvent() {
        val s = suggestTask(event("  "), today, zone)
        assertEquals(SuggestionKind.EVENT, s.kind)
    }

    @Test fun taskIsBuiltWithUtcMidnightDueDate() {
        val e = event("Cumple de Ana")
        val task = suggestionToTask(e, suggestTask(e, today, zone))
        assertEquals(today.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), task.dueDate)
        assertEquals("Del calendario (Personal): Cumple de Ana", task.notes)
    }

    @Test fun pruneForgetsOldKeysAndMalformedOnes() {
        val old = eventKey(event("a", today.minusDays(100), id = 1))
        val recent = eventKey(event("b", today, id = 2))
        val cutoff = today.minusDays(60).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(setOf(recent), pruneImported(setOf(old, recent, "garbage"), cutoff))
    }
}
