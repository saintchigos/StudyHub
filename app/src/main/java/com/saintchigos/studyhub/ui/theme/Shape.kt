package com.saintchigos.studyhub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One set of corner sizes for the whole app.
 *
 * Centralised so cards, sheets and dialogs all agree. The default Material 3 shapes
 * are fine but slightly soft for a timetable, which reads better with a touch more
 * definition on the large components.
 */
val StudyHubShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)