package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The colourful card at the top of Home.
 *
 * Built from the theme's own primary and tertiary colours rather than fixed ones, so
 * it follows whichever accent the student picked, and the text on it is the theme's
 * `onPrimary`, which the palette generator already guarantees is readable.
 *
 * @param nextLine the one thing coming up, such as "Next: STA301 in 25 min". Left out
 * when there is nothing ahead, rather than showing an empty box.
 */
@Composable
fun HomeHero(
    greeting: String,
    date: String,
    nextLine: String?,
    nextDetail: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = scheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(scheme.primary, lerp(scheme.primary, scheme.tertiary, 0.65f))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                // Icons draw in the current content colour, so the bell is told to use
                // the hero's text colour instead of the page's.
                CompositionLocalProvider(LocalContentColor provides textColor) {
                    trailing()
                }
            }

            if (nextLine != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = textColor.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = nextLine,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        if (!nextDetail.isNullOrBlank()) {
                            Text(
                                text = nextDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Good morning", "Good afternoon" or "Good evening" for an hour of the day, 0 to 23. */
fun greetingFor(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 18 -> "Good afternoon"
    else -> "Good evening"
}
