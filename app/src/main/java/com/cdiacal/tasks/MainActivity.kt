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
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.tasks.ui.AppRoot
import com.cdiacal.tasks.ui.TasksViewModel
import com.cdiacal.tasks.ui.theme.LocalEsModoOscuro
import com.cdiacal.tasks.ui.theme.TasksTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TasksViewModel by viewModels()

    /** Incremented each time the widget asks to create a new task. */
    private var newTaskTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_NEW_TASK, false)) newTaskTrigger++
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
                AppRoot(viewModel, newTaskTrigger)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NEW_TASK, false)) newTaskTrigger++
    }

    companion object {
        const val EXTRA_NEW_TASK = "new_task"
    }
}
