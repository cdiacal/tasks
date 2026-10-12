package com.cdiacal.tasks

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.tasks.data.Task
import com.cdiacal.tasks.ui.AppRoot
import com.cdiacal.tasks.ui.TasksViewModel
import com.cdiacal.tasks.ui.sharedTextToDraft
import com.cdiacal.tasks.ui.theme.LocalEsModoOscuro
import com.cdiacal.tasks.ui.theme.TasksTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TasksViewModel by viewModels()

    /** Incremented each time the widget asks to create a new task. */
    private var newTaskTrigger by mutableIntStateOf(0)

    /** Text shared from another app, to prefill the next new task (null for a plain new task). */
    private var sharedDraft by mutableStateOf<Task?>(null)

    /** Incremented each time a habit notification asks to show the Habits tab. */
    private var openHabitsTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val mode by viewModel.themeMode.collectAsStateWithLifecycle()
            TasksTheme(mode) {
                val dark = LocalEsModoOscuro.current
                // The hero header is always dark, so status-bar icons are always light; the
                // navigation bar follows the *app* theme, which can differ from the system one.
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = if (dark) {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        },
                    )
                    onDispose {}
                }
                AppRoot(viewModel, newTaskTrigger, openHabitsTrigger, sharedDraft)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val draft = sharedTextToDraft(
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            )
            if (draft != null) {
                sharedDraft = draft
                newTaskTrigger++
            }
        } else if (intent.getBooleanExtra(EXTRA_NEW_TASK, false)) {
            sharedDraft = null
            newTaskTrigger++
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_HABITS, false)) openHabitsTrigger++
    }

    companion object {
        const val EXTRA_NEW_TASK = "new_task"
        const val EXTRA_OPEN_HABITS = "open_habits"
    }
}
