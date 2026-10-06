package com.saintchigos.studyhub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/**
 * True black for OLED panels.
 *
 * On the OLED screens most students carry, a black pixel is an off pixel, so an
 * always-dark screen costs noticeably less battery. Only applied on top of the
 * chosen dark scheme, so the accent colours are still generated as normal and
 * nothing else changes.
 */
private val TrueBlack = Color(0xFF000000)

internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = TrueBlack,
    surface = TrueBlack,
    surfaceContainerLowest = TrueBlack,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceVariant = Color(0xFF1A1A1A),
)

/**
 * Applies the app theme.
 *
 * The scheme is resolved in one place so that every screen, dialog and sheet agrees.
 * The three inputs are independent on purpose: [darkTheme] decides light or dark,
 * [dynamicColor] decides whether the phone's wallpaper wins over the accent, and
 * [amoled] is a battery tweak that only applies once it is already dark.
 */
@Composable
fun StudyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.DEFAULT,
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val (light, dark) = accentSchemes(accent)

    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        darkTheme -> dark
        else -> light
    }.let { if (darkTheme && amoled) it.toAmoled() else it }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent rather than a solid scheme colour: the app draws edge to
            // edge, so tinting the bars paints a seam across the top of the content.
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            // Dark bars want light icons, so the flag follows darkTheme directly.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Text size is scaled here rather than per screen so a slider actually reaches
    // every composable, including ones Material draws for us.
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density, base.fontScale * fontScale)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StudyHubTypography,
            shapes = StudyHubShapes,
            content = content,
        )
    }
}

/**
 * The fixed pair, kept for the alarm ring screen and anything that must not depend on
 * the student's accent, such as a screen shown while the app is being unlocked.
 */
val FallbackDarkColors: ColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = BlueGrey80,
    error = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

/** Light counterpart of [FallbackDarkColors]. */
val FallbackLightColors: ColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = BlueGrey40,
    error = ErrorRed,
)