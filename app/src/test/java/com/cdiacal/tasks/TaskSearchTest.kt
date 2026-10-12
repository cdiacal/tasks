package com.cdiacal.tasks

import com.cdiacal.tasks.ui.matchesQuery
import com.cdiacal.tasks.ui.normalizeForSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSearchTest {
    @Test fun blankQueryMatchesEverything() {
        assertTrue(matchesQuery("", "cualquier cosa"))
        assertTrue(matchesQuery("   ", "x"))
    }

    @Test fun ignoresCaseAndAccents() {
        assertTrue(matchesQuery("cafe", "Comprar CAFÉ para la oficina"))
        assertTrue(matchesQuery("CAFÉ", "comprar cafe"))
        assertEquals("nino ano", normalizeForSearch("Niño AÑO"))
    }

    @Test fun everyWordMustAppearInAnyOrder() {
        assertTrue(matchesQuery("banco llamar", "Llamar al banco"))
        assertFalse(matchesQuery("banco dentista", "Llamar al banco"))
    }

    @Test fun searchesAcrossAllFields() {
        // title, notes and category name are separate fields of the same task
        assertTrue(matchesQuery("trabajo informe", "Entregar informe", "antes del viernes", "Trabajo"))
    }

    @Test fun noMatchWhenAbsent() = assertFalse(matchesQuery("xyz", "Llamar", "al banco"))
}
