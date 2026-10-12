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
)

/** One row per (habit, day) that was completed. Days are local epoch days. */
@Entity(tableName = "habit_checks", primaryKeys = ["habitId", "epochDay"])
data class HabitCheck(
    val habitId: Long,
    val epochDay: Long,
)
