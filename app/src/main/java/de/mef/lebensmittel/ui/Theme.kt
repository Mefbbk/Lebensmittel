package de.mef.lebensmittel.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B3F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3F0BE),
    onPrimaryContainer = Color(0xFF00210B),
    secondaryContainer = Color(0xFFD5E8D4),
    onSecondaryContainer = Color(0xFF111F12),
    background = Color(0xFFFCFDF9),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF9),
    onSurface = Color(0xFF1A1C19),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF97D5A3),
    onPrimary = Color(0xFF003916),
    primaryContainer = Color(0xFF145228),
    onPrimaryContainer = Color(0xFFB3F0BE),
    secondaryContainer = Color(0xFF3A4B3B),
    onSecondaryContainer = Color(0xFFD5E8D4),
)

/**
 * [mode]: "light" (default), "dark" or "system".
 * [backgroundDark]: when a background picture is set, its brightness decides instead:
 * dark picture -> light text, light picture -> dark text.
 */
@Composable
fun LebensmittelTheme(mode: String = "light", backgroundDark: Boolean? = null, content: @Composable () -> Unit) {
    val dark = backgroundDark ?: when (mode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
