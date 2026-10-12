package com.cdiacal.tasks

import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.data.postponeToTomorrow
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PostponeTest {
    private val day = 24L * 60 * 60 * 1000
    private val today = LocalDate.of(2024, 5, 15).toEpochDay() * day

    @Test fun overdueTaskMovesToTomorrowAndItsReminderFollows() {
        val reminder = today - 3 * day + 9 * 3_600_000L
        val t = Task(title = "t", dueDate = today - 3 * day, reminderAt = reminder)
        val moved = postponeToTomorrow(t, today, ZoneOffset.UTC)
        assertEquals(today + day, moved.dueDate)
        assertEquals(reminder + 4 * day, moved.reminderAt) // 3 days overdue -> 4 days later
    }

    @Test fun taskDueTodayMovesOneDay() {
        val moved = postponeToTomorrow(Task(title = "t", dueDate = today), today, ZoneOffset.UTC)
        assertEquals(today + day, moved.dueDate)
    }

    @Test fun taskWithoutDueDateIsUntouched() {
        val t = Task(title = "t")
        assertEquals(t, postponeToTomorrow(t, today, ZoneOffset.UTC))
    }
}
