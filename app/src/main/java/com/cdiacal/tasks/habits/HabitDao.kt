package com.cdiacal.tasks.habits

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC, id ASC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): Habit?

    @Query("SELECT * FROM habit_checks")
    fun observeChecks(): Flow<List<HabitCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId")
    suspend fun deleteChecksFor(habitId: Long)

    @Query("SELECT COUNT(*) FROM habit_checks WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun isChecked(habitId: Long, epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheck)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun uncheck(habitId: Long, epochDay: Long)

    // Used by backup/restore.
    @Query("SELECT * FROM habits")
    suspend fun getAllHabits(): List<Habit>

    @Query("SELECT * FROM habit_checks")
    suspend fun getAllChecks(): List<HabitCheck>

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_checks")
    suspend fun deleteAllChecks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<Habit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChecks(checks: List<HabitCheck>)
}
