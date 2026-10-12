package com.cdiacal.tasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.tasks.widget.createHabitRepository
import com.cdiacal.tasks.widget.createRepository
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val tasks = createRepository(application)
    private val habits = createHabitRepository(application)

    /** Today's local epoch day, re-checked every minute so the charts roll over at midnight. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now().toEpochDay())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<StatsState> = combine(
        tasks.tasks, tasks.completions, habits.habits, habits.checks, today,
    ) { t, completions, h, checks, day ->
        buildStats(t, completions, h, checks, day, reminderToUtcDateMillis(System.currentTimeMillis()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}
