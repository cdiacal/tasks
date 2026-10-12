package com.cdiacal.tasks.habits

import androidx.room.withTransaction
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val db: TaskDatabase,
    private val reminders: HabitReminderScheduler,
) {
    private val dao = db.habitDao()

    val habits: Flow<List<Habit>> = dao.observeHabits()
    val checks: Flow<List<HabitCheck>> = dao.observeChecks()

    suspend fun save(habit: Habit) {
        val id = dao.upsert(habit)
        reminders.sync(habit.copy(id = id))
    }

    /** Deleting a habit also deletes its whole history. */
    suspend fun delete(habit: Habit) = db.withTransaction {
        dao.deleteChecksFor(habit.id)
        dao.deleteHabit(habit.id)
        reminders.cancel(habit.id)
    }

    /** Marks the day done, or undoes it when it was already done. */
    suspend fun toggle(habitId: Long, epochDay: Long) = db.withTransaction {
        if (dao.isChecked(habitId, epochDay) > 0) {
            dao.uncheck(habitId, epochDay)
        } else {
            dao.insertCheck(HabitCheck(habitId, epochDay))
        }
    }
}
