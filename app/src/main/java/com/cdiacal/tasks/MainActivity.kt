package com.cdiacal.tasks

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.tasks.ui.TasksScreen
import com.cdiacal.tasks.ui.TasksViewModel
import com.cdiacal.tasks.ui.theme.LocalEsModoOscuro
import com.cdiacal.tasks.ui.theme.TasksTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TasksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                TasksScreen(viewModel)
            }
        }
    }
}
