package com.cdiacal.tasks.ui.habits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.habits.Habit
import com.cdiacal.tasks.habits.HabitsState
import com.cdiacal.tasks.habits.buildHabitsState
import com.cdiacal.tasks.widget.createHabitRepository
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = createHabitRepository(application)

    /** Today's local epoch day, re-checked every minute so the panel rolls over at midnight. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now().toEpochDay())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<HabitsState> =
        combine(repository.habits, repository.checks, today) { habits, checks, day ->
            buildHabitsState(habits, checks, day)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun toggle(habitId: Long, epochDay: Long) = viewModelScope.launch { repository.toggle(habitId, epochDay) }

    fun save(habit: Habit) = viewModelScope.launch { repository.save(habit) }

    fun delete(habit: Habit) = viewModelScope.launch { repository.delete(habit) }
}
