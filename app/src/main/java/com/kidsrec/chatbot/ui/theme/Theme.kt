package com.kidsrec.chatbot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colour palette used when the device is in dark mode
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFFCE93D8),
    tertiary = Color(0xFFFF8A65)
)

// Colour palette used when the device is in light mode
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5C6BC0),
    secondary = Color(0xFFAB47BC),
    tertiary = Color(0xFFFF7043),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
)

// Main app theme wrapper used throughout the LittleDino application
@Composable
fun KidsRecommendationAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Selects the correct colour scheme based on dark/light mode
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Gets the current Android view so system bar colours can be updated
    val view = LocalView.current
    // Prevents preview/edit mode from trying to access the actual Activity window
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Sets the status bar colour to match the app's primary colour
            window.statusBarColor = colorScheme.primary.toArgb()
            // Controls whether status bar icons should appear light or dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Applies the selected colour scheme and typography to all child composables
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
