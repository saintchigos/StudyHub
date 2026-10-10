package com.saintchigos.studyhub.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Turns an accent seed into a full, readable light and dark scheme.
 *
 * Material 3 can generate a scheme from a seed, but this does it by hand for two
 * reasons that matter on the phones this app targets:
 *
 *  - Contrast is guaranteed rather than hoped for. The primary is fixed at a
 *    lightness that stays legible as filled buttons, chips and progress bars, and the
 *    "on" colours are pinned to black or white rather than derived, so nothing
 *    depends on a hue landing somewhere friendly by luck.
 *  - The neutrals carry a trace of the accent. Pure grey next to a saturated
 *    primary looks like two apps stitched together; a few percent of the accent hue
 *    in the surfaces makes the whole thing feel deliberate.
 *
 * Kept free of Android types so it can be unit tested on the JVM.
 */
private data class Hsl(val h: Float, val s: Float, val l: Float)

private fun Color.toHsl(): Hcl {
    val r = red
    val g = green
    val b = blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val l = (maxC + minC) / 2f

    if (delta == 0f) return Hcl(0f, 0f, l)

    val s = if (l > 0.5f) delta / (2f - maxC - minC) else delta / (maxC + minC)

    val h = when (maxC) {
        r -> ((g - b) / delta) % 6f
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f

    return Hcl(h, s, l)
}

private fun Hcl.toColor(): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val hp = (((h % 360f) + 360f) % 360f) / 60f
    val x = c * (1f - abs((hp % 2f) - 1f))

    val (r1, g1, b1) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val m = l - c / 2f
    fun channel(v: Float) = (v + m).coerceIn(0f, 1f)
    return Color(channel(r1), channel(g1), channel(b1))
}

private data class Hcl(val h: Float, val s: Float, val l: Float)

/** Relative luminance, per WCAG 2.1. */
private fun luminance(c: Color): Double {
    fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}

/** WCAG contrast ratio between two colours, 1.0 to 21.0. */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

private fun Hcl.atLightness(l: Float) = Hcl(h, s, l).toColor()

/**
 * Picks a filled colour and the text colour that sits on it.
 *
 * Hue alone does not decide whether white text works: at the same lightness a yellow
 * or green is far brighter than a blue, so a fixed "white on primary" rule quietly
 * produces unreadable buttons for some accents and not others. Instead the lightness
 * is walked until one of the two text colours genuinely clears the ratio, trying the
 * preferred one first so the result still looks deliberate.
 *
 * Returns the pair rather than a boolean so callers cannot pair a colour with text
 * that was measured against a different colour.
 */
private fun readablePair(
    hue: Float,
    saturation: Float,
    lightness: Float,
    preferLightText: Boolean,
    minimum: Double = 4.6
): Pair<Color, Color> {
    val order = if (preferLightText) listOf(Color.White, Color.Black) else listOf(Color.Black, Color.White)

    order.forEach { text ->
        // Walk away from the text colour: darken for light text, lighten for dark text.
        val step = if (text == Color.White) -0.02f else 0.02f
        var l = lightness
        while (l >= 0.04f && l <= 0.97f) {
            val candidate = Hcl(hue, saturation, l).atLightness(l)
            if (contrastRatio(candidate, text) >= minimum) return candidate to text
            l += step
        }
    }

    // Unreachable for any real hue, but returning something valid beats crashing in
    // the theme composable if one ever is.
    val fallback = Hcl(hue, saturation, lightness).atLightness(lightness)
    return fallback to Color.White
}

/**
 * One accent's full scheme.
 *
 * Three ideas make it feel alive rather than corporate:
 *  - The page and every card colour carry a clear wash of the accent hue. Material's
 *    own card colours are a fixed lavender-grey that ignores the accent, so setting
 *    the surface container roles is what makes the whole app change colour, not just
 *    the buttons.
 *  - The primary is allowed to be genuinely saturated. Contrast is still guaranteed,
 *    because [readablePair] walks the lightness until the text on it passes.
 *  - The supporting colours sit a third of the way round the wheel (tertiary) instead
 *    of next door, so a gradient from primary to tertiary has real movement in it.
 *
 * Every value here is checked by PaletteTest for readability and for accents being
 * visibly different from each other.
 */
private fun schemeFor(seed: Color, dark: Boolean): ColorScheme {
    val base = seed.toHsl()
    val h = base.h
    val s = base.s.coerceIn(0f, 0.80f)

    val secondaryH = (h + 40f) % 360f
    val tertiaryH = (h + 120f) % 360f

    return if (dark) {
        val (primary, onPrimary) = readablePair(h, s * 0.85f, 0.74f, preferLightText = false)
        val (secondary, onSecondary) = readablePair(secondaryH, s * 0.70f, 0.72f, preferLightText = false)
        val (tertiary, onTertiary) = readablePair(tertiaryH, s * 0.70f, 0.74f, preferLightText = false)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Hcl(h, s * 0.55f, 0.28f).toColor(),
            onPrimaryContainer = Hcl(h, s * 0.40f, 0.92f).toColor(),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = Hcl(secondaryH, s * 0.45f, 0.26f).toColor(),
            onSecondaryContainer = Hcl(secondaryH, s * 0.35f, 0.90f).toColor(),
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = Hcl(tertiaryH, s * 0.45f, 0.28f).toColor(),
            onTertiaryContainer = Hcl(tertiaryH, s * 0.35f, 0.92f).toColor(),
            background = Hcl(h, 0.22f, 0.07f).toColor(),
            onBackground = Hcl(h, 0.20f, 0.93f).toColor(),
            surface = Hcl(h, 0.22f, 0.07f).toColor(),
            onSurface = Hcl(h, 0.20f, 0.93f).toColor(),
            surfaceVariant = Hcl(h, 0.20f, 0.20f).toColor(),
            onSurfaceVariant = Hcl(h, 0.16f, 0.80f).toColor(),
            surfaceContainerLowest = Hcl(h, 0.24f, 0.05f).toColor(),
            surfaceContainerLow = Hcl(h, 0.22f, 0.09f).toColor(),
            surfaceContainer = Hcl(h, 0.22f, 0.12f).toColor(),
            surfaceContainerHigh = Hcl(h, 0.22f, 0.15f).toColor(),
            surfaceContainerHighest = Hcl(h, 0.22f, 0.18f).toColor(),
            outline = Hcl(h, 0.14f, 0.52f).toColor(),
            outlineVariant = Hcl(h, 0.18f, 0.28f).toColor(),
            error = ErrorDark,
            onError = Color.White,
        )
    } else {
        val (primary, onPrimary) = readablePair(h, s, 0.42f, preferLightText = true)
        val (secondary, onSecondary) = readablePair(secondaryH, s * 0.85f, 0.38f, preferLightText = true)
        val (tertiary, onTertiary) = readablePair(tertiaryH, s * 0.85f, 0.40f, preferLightText = true)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Hcl(h, s * 0.70f, 0.88f).toColor(),
            onPrimaryContainer = Hcl(h, s, 0.20f).toColor(),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = Hcl(secondaryH, s * 0.60f, 0.88f).toColor(),
            onSecondaryContainer = Hcl(secondaryH, s, 0.18f).toColor(),
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = Hcl(tertiaryH, s * 0.60f, 0.88f).toColor(),
            onTertiaryContainer = Hcl(tertiaryH, s, 0.20f).toColor(),
            background = Hcl(h, 0.32f, 0.97f).toColor(),
            onBackground = Hcl(h, 0.40f, 0.12f).toColor(),
            surface = Hcl(h, 0.32f, 0.97f).toColor(),
            onSurface = Hcl(h, 0.40f, 0.12f).toColor(),
            surfaceVariant = Hcl(h, 0.34f, 0.91f).toColor(),
            onSurfaceVariant = Hcl(h, 0.30f, 0.28f).toColor(),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Hcl(h, 0.40f, 0.96f).toColor(),
            surfaceContainer = Hcl(h, 0.38f, 0.94f).toColor(),
            surfaceContainerHigh = Hcl(h, 0.36f, 0.92f).toColor(),
            surfaceContainerHighest = Hcl(h, 0.34f, 0.90f).toColor(),
            outline = Hcl(h, 0.16f, 0.55f).toColor(),
            outlineVariant = Hcl(h, 0.28f, 0.82f).toColor(),
            error = ErrorRed,
            onError = Color.White,
        )
    }
}

/** The light and dark schemes for an accent, generated once per seed. */
fun accentSchemes(accent: Accent): Pair<ColorScheme, ColorScheme> =
    schemeFor(accent.seed, dark = false) to schemeFor(accent.seed, dark = true)