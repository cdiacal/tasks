package com.cdiacal.tasks

import com.cdiacal.tasks.backup.autoBackupFileName
import com.cdiacal.tasks.backup.selectOldBackups
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoBackupTest {
    @Test fun fileNameUsesTheIsoDate() =
        assertEquals("tareas-auto-2026-10-12.json", autoBackupFileName(LocalDate.of(2026, 10, 12)))

    @Test fun keepsTheNewestAndSelectsTheRestForDeletion() {
        val names = listOf(
            "tareas-auto-2026-09-01.json",
            "tareas-auto-2026-10-12.json",
            "tareas-auto-2026-09-15.json",
            "tareas-auto-2026-10-05.json",
            "tareas-auto-2026-09-22.json",
            "tareas-auto-2026-09-29.json",
            "tareas-auto-2026-10-19.json",
        )
        assertEquals(
            listOf("tareas-auto-2026-09-15.json", "tareas-auto-2026-09-01.json"),
            selectOldBackups(names, keep = 5),
        )
    }

    @Test fun neverTouchesFilesThatAreNotAutomaticBackups() {
        val names = listOf("tareas-2026-10-12.json", "foto.jpg", "tareas-auto-borrador.json", "tareas-auto-2026-10-12.json")
        assertEquals(emptyList<String>(), selectOldBackups(names, keep = 5))
        assertEquals(listOf("tareas-auto-2026-10-12.json"), selectOldBackups(names, keep = 0))
    }

    @Test fun nothingToDeleteWhenUnderTheLimit() =
        assertEquals(emptyList<String>(), selectOldBackups(listOf("tareas-auto-2026-10-12.json"), keep = 5))
}
