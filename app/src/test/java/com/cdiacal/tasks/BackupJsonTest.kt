package com.cdiacal.tasks

import com.cdiacal.tasks.data.BackupJson
import com.cdiacal.tasks.data.Category
import com.cdiacal.tasks.data.InvalidBackupException
import com.cdiacal.tasks.data.Subtask
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupJsonTest {
    private val categories = listOf(Category(id = 3, name = "Trabajo", emoji = "💼"), Category(id = 4, name = "Casa"))
    private val tasks = listOf(
        Task(id = 1, title = "Llamar", notes = "al banco\ncon \"comillas\"", isDone = false, dueDate = 1_000L, reminderAt = 2_000L, categoryId = 3, createdAt = 10),
        Task(id = 2, title = "Comprar pan", isDone = true, createdAt = 20),
    )

    @Test fun roundTripPreservesEverything() {
        val data = BackupJson.decode(BackupJson.encode(tasks, categories, exportedAt = 99))
        assertEquals(categories, data.categories)
        assertEquals(tasks, data.tasks)
    }

    @Test fun habitsAndChecksRoundTrip() {
        val habits = listOf(Habit(id = 5, name = "Leer", emoji = "📚", daysMask = 0b0011111, createdAt = 123, reminderMinutes = 540))
        val checks = listOf(HabitCheck(5, 20_000), HabitCheck(5, 20_001))
        val data = BackupJson.decode(BackupJson.encode(tasks, categories, 0, habits, checks))
        assertEquals(habits, data.habits)
        assertEquals(checks, data.habitChecks)
    }

    @Test fun habitSlotsAndSlotChecksRoundTrip() {
        val habits = listOf(Habit(id = 7, name = "Pastilla", createdAt = 1, slotsMask = 0b101))
        val checks = listOf(HabitCheck(7, 20_000, 1), HabitCheck(7, 20_000, 3))
        val data = BackupJson.decode(BackupJson.encode(tasks, categories, 0, habits, checks))
        assertEquals(habits, data.habits)
        assertEquals(checks, data.habitChecks)
    }

    @Test fun oldBackupChecksWithoutSlotDecodeAsWholeDayTicks() {
        val json = """{"app":"tareas","version":1,"tasks":[],"categories":[],"habits":[{"id":1,"name":"a","createdAt":1}],""" +
            """"habitChecks":[{"habitId":1,"epochDay":5}]}"""
        val data = BackupJson.decode(json)
        assertEquals(0, data.habits!!.single().slotsMask)
        assertEquals(0, data.habitChecks.single().slot)
    }

    @Test fun backupWithoutHabitsSectionDecodesToNull() {
        val json = """{"app":"tareas","version":1,"tasks":[],"categories":[]}"""
        assertNull(BackupJson.decode(json).habits)
    }

    @Test fun habitWithoutReminderKeepsItNull() {
        val habit = Habit(id = 6, name = "Agua", createdAt = 5)
        val data = BackupJson.decode(BackupJson.encode(tasks, categories, 0, listOf(habit), emptyList()))
        assertNull(data.habits!!.single().reminderMinutes)
    }

    @Test fun recurrenceAndSubtasksRoundTrip() {
        val repeating = Task(id = 9, title = "Alquiler", dueDate = 5_000L, recurrence = "MONTHLY", createdAt = 1)
        val steps = listOf(
            Subtask(id = 1, taskId = 9, title = "Transferencia", isDone = true, position = 0),
            Subtask(id = 2, taskId = 9, title = "Guardar recibo", isDone = false, position = 1),
        )
        val data = BackupJson.decode(BackupJson.encode(listOf(repeating), emptyList(), 0, subtasks = steps))
        assertEquals("MONTHLY", data.tasks.single().recurrence)
        assertEquals(steps, data.subtasks)
    }

    @Test fun oldBackupsDefaultToNoRepeatAndNoSteps() {
        val json = """{"app":"tareas","version":1,"tasks":[{"id":1,"title":"x"}]}"""
        val data = BackupJson.decode(json)
        assertEquals("NONE", data.tasks.single().recurrence)
        assertEquals(0, data.subtasks.size)
    }

    @Test fun nullFieldsStayNull() {
        val task = BackupJson.decode(BackupJson.encode(tasks, categories, 0)).tasks[1]
        assertNull(task.dueDate)
        assertNull(task.reminderAt)
        assertNull(task.categoryId)
    }

    @Test fun emptyBackupIsValid() {
        val data = BackupJson.decode(BackupJson.encode(emptyList(), emptyList(), 0))
        assertEquals(0, data.tasks.size)
        assertEquals(0, data.categories.size)
    }

    @Test fun missingOptionalFieldsGetDefaults() {
        val json = """{"app":"tareas","version":1,"tasks":[{"id":7,"title":"x"}]}"""
        val task = BackupJson.decode(json).tasks.single()
        assertEquals("", task.notes)
        assertEquals(false, task.isDone)
        assertNull(task.dueDate)
    }

    @Test(expected = InvalidBackupException::class)
    fun rejectsOtherApps() { BackupJson.decode("""{"app":"otra","version":1}""") }

    @Test(expected = InvalidBackupException::class)
    fun rejectsNewerVersions() { BackupJson.decode("""{"app":"tareas","version":99}""") }

    @Test(expected = InvalidBackupException::class)
    fun rejectsGarbage() { BackupJson.decode("esto no es json") }

    @Test(expected = InvalidBackupException::class)
    fun rejectsTasksMissingRequiredFields() {
        BackupJson.decode("""{"app":"tareas","version":1,"tasks":[{"id":1}]}""")
    }
}
