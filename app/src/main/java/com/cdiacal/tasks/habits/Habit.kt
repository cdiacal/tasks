package com.cdiacal.tasks.habits

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bit i (0 = Monday .. 6 = Sunday) is set when the habit is scheduled that weekday. */
const val ALL_DAYS = 0b1111111

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "",
    val daysMask: Int = ALL_DAYS,
    val createdAt: Long = System.currentTimeMillis(),
    /** Daily reminder time as minutes after local midnight, or null for no reminder. */
    val reminderMinutes: Int? = null,
    /** Bit i set = the habit is done in [DaySlot] i+1 (morning, noon, night); 0 = once a day, any time. */
    val slotsMask: Int = 0,
    /** Reminder time of each day slot, as "slot=minutes;slot=minutes" (see [parseSlotReminders]). */
    val slotReminders: String = "",
)

/**
 * One row per (habit, day, slot) that was completed. Days are local epoch days. [slot] is a
 * [DaySlot] value, or 0 for a plain "done today" tick (habits without day slots, and ticks made
 * before slots existed).
 */
@Entity(tableName = "habit_checks", primaryKeys = ["habitId", "epochDay", "slot"])
data class HabitCheck(
    val habitId: Long,
    val epochDay: Long,
    val slot: Int = 0,
)
