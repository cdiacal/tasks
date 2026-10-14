package com.cdiacal.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.habits.HabitsScreen

/** Two top-level sections (Tasks / Habits) switched with a flat bottom bar. */
@Composable
fun AppRoot(
    tasksViewModel: TasksViewModel,
    newTaskTrigger: Int,
    openHabitsTrigger: Int = 0,
    sharedDraft: Task? = null,
    newHabitTrigger: Int = 0,
    openCalendarTrigger: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // The widget's "+" must land on the Tasks tab, and be consumed once so that coming back
    // to that tab later does not reopen the dialog.
    var handledTrigger by remember { mutableIntStateOf(0) }
    var handledHabitTrigger by remember { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    // The full-screen task editor covers the bottom bar so a tab switch cannot discard an edit.
    var editorOpen by remember { mutableStateOf(false) }
    LaunchedEffect(newTaskTrigger) {
        if (newTaskTrigger > handledTrigger) tab = 0
    }

    LaunchedEffect(openCalendarTrigger) {
        if (openCalendarTrigger > 0) showCalendar = true
    }

    LaunchedEffect(openHabitsTrigger) {
        if (openHabitsTrigger > 0) tab = 1
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                TasksScreen(
                    viewModel = tasksViewModel,
                    newTaskTrigger = if (newTaskTrigger > handledTrigger) newTaskTrigger else 0,
                    sharedDraft = sharedDraft,
                    onNewTaskConsumed = { handledTrigger = newTaskTrigger },
                    onOpenSettings = { showSettings = true },
                    onEditorVisibleChange = { editorOpen = it },
                )
            } else if (tab == 2) {
                TaskCalendarScreen(tasksViewModel, onOpenSettings = { showSettings = true })
            } else if (tab == 1) {
                HabitsScreen(
                    onOpenSettings = { showSettings = true },
                    newHabitTrigger = if (newHabitTrigger > handledHabitTrigger) newHabitTrigger else 0,
                    onNewHabitConsumed = { handledHabitTrigger = newHabitTrigger },
                )
            } else {
                StatsScreen(onOpenSettings = { showSettings = true })
            }
        }
        if (!editorOpen) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NavigationBarItem(
                selected = tab == 0,
                onClick = { tab = 0 },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                label = { Text(stringResource(R.string.tab_tasks)) },
                colors = colors,
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { tab = 1 },
                icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) },
                label = { Text(stringResource(R.string.tab_habits)) },
                colors = colors,
            )
            NavigationBarItem(
                selected = tab == 2,
                onClick = { tab = 2 },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                label = { Text(stringResource(R.string.tab_calendar)) },
                colors = colors,
            )
            NavigationBarItem(
                selected = tab == 3,
                onClick = { tab = 3 },
                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                label = { Text(stringResource(R.string.tab_stats)) },
                colors = colors,
            )
        }
        }
    }
    if (showSettings) SettingsScreen(tasksViewModel, onBack = { showSettings = false }, onOpenCalendar = { showCalendar = true })
    if (showCalendar) CalendarPreviewScreen(tasksViewModel, onBack = { showCalendar = false })
    }
}
