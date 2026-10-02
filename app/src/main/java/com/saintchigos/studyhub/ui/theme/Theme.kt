package com.saintchigos.studyhub.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = BlueGrey80,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    error = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

private val LightColors = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = BlueGrey40,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightVariant,
    error = ErrorRed,
)

@Composable
fun StudyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyHubTypography,
        content = content
    )
}