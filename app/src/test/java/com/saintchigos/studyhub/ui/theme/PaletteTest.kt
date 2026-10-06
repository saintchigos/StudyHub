package com.saintchigos.studyhub.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Guards the hand built accent palettes.
 *
 * The scheme is generated from a seed rather than hand written per accent, which
 * means a bad seed silently produces unreadable buttons instead of failing to
 * compile. These tests are the only thing standing between a new accent and white
 * text on a white button.
 */
class PaletteTest {

    /** Relative luminance, per WCAG 2.1. */
    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    /** WCAG contrast ratio, 1.0 to 21.0. */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test
    fun everyAccentKeepsTextReadableOnItsFilledButtons() {
        Accent.entries.forEach { accent ->
            val (light, dark) = accentSchemes(accent)

            // 4.5 is the WCAG AA threshold for body text, which is what a button
            // label is. Anything under this is a button nobody can read.
            listOf(
                "light primary" to contrast(light.onPrimary, light.primary),
                "light secondary" to contrast(light.onSecondary, light.secondary),
                "light tertiary" to contrast(light.onTertiary, light.tertiary),
                "dark primary" to contrast(dark.onPrimary, dark.primary),
                "dark secondary" to contrast(dark.onSecondary, dark.secondary),
                "dark tertiary" to contrast(dark.onTertiary, dark.tertiary),
            ).forEach { (what, ratio) ->
                assertTrue(
                    "$accent $what contrast was $ratio, needs 4.5",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun everyAccentKeepsBodyTextReadableOnItsSurfaces() {
        Accent.entries.forEach { accent ->
            val (light, dark) = accentSchemes(accent)
            listOf(
                "light onSurface" to contrast(light.onSurface, light.surface),
                "light onSurfaceVariant" to contrast(light.onSurfaceVariant, light.surfaceVariant),
                "dark onSurface" to contrast(dark.onSurface, dark.surface),
                "dark onSurfaceVariant" to contrast(dark.onSurfaceVariant, dark.surfaceVariant),
            ).forEach { (what, ratio) ->
                assertTrue(
                    "$accent $what contrast was $ratio, needs 4.5",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun primaryStandsOutFromTheSurfaceBehindIt() {
        // If the primary blended into the background, filled buttons would vanish
        // into the page. 1.6 is a deliberately loose bar: this is about separation,
        // not readability.
        Accent.entries.forEach { accent ->
            val (light, dark) = accentSchemes(accent)
            assertTrue(
                "${accent.name} primary too close to light surface",
                contrast(light.primary, light.surface) >= 1.6
            )
            assertTrue(
                "${accent.name} primary too close to dark surface",
                contrast(dark.primary, dark.surface) >= 1.6
            )
        }
    }

    @Test
    fun darkSchemeIsActuallyDarkAndLightSchemeActuallyLight() {
        Accent.entries.forEach { accent ->
            val (light, dark) = accentSchemes(accent)
            assertTrue("${accent.name} light scheme is not light", luminance(light.surface) > 0.6)
            assertTrue("${accent.name} dark scheme is not dark", luminance(dark.surface) < 0.1)
        }
    }

    @Test
    fun amoledOnlyDarkensTheBackgroundAndKeepsTextReadable() {
        Accent.entries.forEach { accent ->
            val (_, dark) = accentSchemes(accent)
            val amoled = dark.toAmoled()
            assertTrue(
                "${accent.name} amoled background is not black",
                luminance(amoled.background) < 0.01
            )
            // The whole point is saving power, so the text has to stay legible on
            // true black rather than dimming along with the background.
            assertTrue(
                "${accent.name} amoled text contrast was ${contrast(amoled.onSurface, amoled.background)}",
                contrast(amoled.onSurface, amoled.background) >= 4.5
            )
        }
    }

    @Test
    fun accentsAreVisuallyDistinctFromEachOther() {
        // Two accents that generate nearly the same primary are a support problem:
        // the student cannot tell which one they picked.
        val primaries = Accent.entries.map { accentSchemes(it).first.primary }
        for (i in primaries.indices) {
            for (j in i + 1 until primaries.size) {
                val a = primaries[i]
                val b = primaries[j]
                val distance = max(
                    kotlin.math.abs(a.red - b.red),
                    max(kotlin.math.abs(a.green - b.green), kotlin.math.abs(a.blue - b.blue))
                )
                assertTrue("accents $i and $j are too similar (distance $distance)", distance > 0.08f)
            }
        }
    }
}