package com.cdiacal.tasks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** True when the app is rendering with the dark palette (which may differ from the system setting). */
val LocalEsModoOscuro = compositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E4B87),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EAF5),
    onPrimaryContainer = Color(0xFF152C52),
    secondary = Color(0xFF12734A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF111420),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111420),
    surfaceVariant = Color(0xFFE6E9F0),
    onSurfaceVariant = Color(0xFF767C90),
    outline = Color(0xFFE6E9F0),
    outlineVariant = Color(0xFFE6E9F0),
    error = Color(0xFFC6473A),
    onError = Color(0xFFFFFFFF),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE6E9F0),
    inverseSurface = Color(0xFF111420),
    inverseOnSurface = Color(0xFFF5F6FA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5A9BF5),
    onPrimary = Color(0xFF0C0E16),
    primaryContainer = Color(0xFF1B2C46),
    onPrimaryContainer = Color(0xFFEDEEF5),
    secondary = Color(0xFF2DD48E),
    onSecondary = Color(0xFF0C0E16),
    background = Color(0xFF0C0E16),
    onBackground = Color(0xFFEDEEF5),
    surface = Color(0xFF161A26),
    onSurface = Color(0xFFEDEEF5),
    surfaceVariant = Color(0xFF232838),
    onSurfaceVariant = Color(0xFF8189A0),
    outline = Color(0xFF232838),
    outlineVariant = Color(0xFF232838),
    error = Color(0xFFF0705F),
    onError = Color(0xFF0C0E16),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF0C0E16),
    surfaceContainerLow = Color(0xFF161A26),
    surfaceContainer = Color(0xFF161A26),
    surfaceContainerHigh = Color(0xFF161A26),
    surfaceContainerHighest = Color(0xFF232838),
    inverseSurface = Color(0xFFEDEEF5),
    inverseOnSurface = Color(0xFF0C0E16),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp), // dialogs
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun TasksTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalEsModoOscuro provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

// --- Mode-dependent tokens: always read colors through these, never hard-code a mode. ---

/** Positive meaning: success / done. */
@Composable fun colorIngreso(): Color = if (LocalEsModoOscuro.current) Color(0xFF2DD48E) else Color(0xFF12734A)

/** Negative meaning: error / overdue. */
@Composable fun colorGasto(): Color = if (LocalEsModoOscuro.current) Color(0xFFF0705F) else Color(0xFFC6473A)

/** Warning: close to the limit. */
@Composable fun colorAviso(): Color = if (LocalEsModoOscuro.current) Color(0xFFE5A93F) else Color(0xFFD9932B)

@Composable fun heroInicio(): Color = if (LocalEsModoOscuro.current) Color(0xFF11223F) else Color(0xFF152C52)

@Composable fun heroFin(): Color = if (LocalEsModoOscuro.current) Color(0xFF183C6B) else Color(0xFF1E4B87)

/** Diagonal navy gradient used by headers and the primary button. */
@Composable fun brushHero(): Brush = Brush.linearGradient(listOf(heroInicio(), heroFin()))

private val IconBgLight = listOf(
    0xFFE3EAF5, 0xFFE0EFE7, 0xFFF5EADF, 0xFFEDE6F3, 0xFFF6E4E4, 0xFFE1EFF2, 0xFFF0EEE3, 0xFFE7EAF0,
).map(::Color)

private val IconBgDark = listOf(
    0xFF1E2A3E, 0xFF1B2F28, 0xFF322A20, 0xFF2A2438, 0xFF352424, 0xFF1C2E33, 0xFF2E2C22, 0xFF242833,
).map(::Color)

/** Deterministic muted tile color for an icon: one of 8 tones chosen by id % 8. */
@Composable
fun colorFondoIcono(id: Long): Color {
    val palette = if (LocalEsModoOscuro.current) IconBgDark else IconBgLight
    return palette[Math.floorMod(id, 8L).toInt()]
}
