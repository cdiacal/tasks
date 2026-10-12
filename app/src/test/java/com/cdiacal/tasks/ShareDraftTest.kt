package com.cdiacal.tasks

import com.cdiacal.tasks.ui.sharedTextToDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareDraftTest {
    @Test fun nothingSharedGivesNoDraft() {
        assertNull(sharedTextToDraft(null, null))
        assertNull(sharedTextToDraft("  ", " \n "))
    }

    @Test fun singleLineBecomesTheTitleOnly() {
        val d = sharedTextToDraft(null, "  Comprar pan ")!!
        assertEquals("Comprar pan", d.title)
        assertEquals("", d.notes)
    }

    @Test fun firstLineIsTitleAndFullTextGoesToNotes() {
        val d = sharedTextToDraft(null, "Reunión\nsala 3, 10:00")!!
        assertEquals("Reunión", d.title)
        assertEquals("Reunión\nsala 3, 10:00", d.notes)
    }

    @Test fun subjectWinsAsTitleAndTextGoesToNotes() {
        val d = sharedTextToDraft("Artículo", "https://example.com/a")!!
        assertEquals("Artículo", d.title)
        assertEquals("https://example.com/a", d.notes)
    }

    @Test fun longTitleIsCutWithEllipsisAndTextKept() {
        val long = "x".repeat(300)
        val d = sharedTextToDraft(null, long)!!
        assertEquals(120, d.title.length)
        assertEquals(true, d.title.endsWith("…"))
        assertEquals(long, d.notes)
    }
}
