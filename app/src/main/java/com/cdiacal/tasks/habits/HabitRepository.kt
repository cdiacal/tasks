package com.cdiacal.tasks.habits

import androidx.room.withTransaction
import com.cdiacal.tasks.data.TaskDatabase
import com.cdiacal.tasks.reminders.HabitReminderScheduler
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val db: TaskDatabase,
    private val reminders: HabitReminderScheduler,
    /** Called after any change to habits or their checks (used to refresh the habits widget). */
    private val onChanged: suspend () -> Unit = {},
) {
    private val dao = db.habitDao()

    val habits: Flow<List<Habit>> = dao.observeHabits()
    val checks: Flow<List<HabitCheck>> = dao.observeChecks()

    suspend fun save(habit: Habit) {
        val id = dao.upsert(habit)
        reminders.sync(habit.copy(id = id))
        onChanged()
    }

    /** Deleting a habit also deletes its whole history. */
    suspend fun delete(habit: Habit) {
        db.withTransaction {
            dao.deleteChecksFor(habit.id)
            dao.deleteHabit(habit.id)
        }
        reminders.cancel(habit.id)
        onChanged()
    }

    /**
     * Marks the whole day done (every slot of the habit), or clears it when it was already done.
     * Used by the week strip and the calendar.
     */
    suspend fun toggle(habitId: Long, epochDay: Long) {
        db.withTransaction {
            val habit = dao.getHabit(habitId)
            if (habit != null) {
                val stored = dao.checksForDay(habitId, epochDay).map { it.slot }.toSet()
                if (isDayComplete(habit.slotsMask, stored)) {
                    dao.uncheck(habitId, epochDay)
                } else if (habit.slotsMask == 0) {
                    dao.insertCheck(HabitCheck(habitId, epochDay, 0))
                } else {
                    slotsOf(habit.slotsMask).forEach { dao.insertCheck(HabitCheck(habitId, epochDay, it.value)) }
                }
            }
        }
        onChanged()
    }

    /** Ticks or unticks one part of the day (morning / noon / night) of a habit. */
    suspend fun toggleSlot(habitId: Long, epochDay: Long, slot: Int) {
        db.withTransaction {
            val habit = dao.getHabit(habitId)
            if (habit != null) {
                val stored = dao.checksForDay(habitId, epochDay).map { it.slot }.toSet()
                when {
                    slot in stored -> dao.uncheckSlot(habitId, epochDay, slot)
                    0 in stored -> {
                        // A whole-day tick from before slots existed: split it into its slots, minus this one.
                        dao.uncheckSlot(habitId, epochDay, 0)
                        slotsOf(habit.slotsMask).filter { it.value != slot }
                            .forEach { dao.insertCheck(HabitCheck(habitId, epochDay, it.value)) }
                    }
                    else -> dao.insertCheck(HabitCheck(habitId, epochDay, slot))
                }
            }
        }
        onChanged()
    }
}
