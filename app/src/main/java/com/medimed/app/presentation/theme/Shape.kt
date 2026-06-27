package com.medimed.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MediMed Shape System
 *
 * Standardized corner radii used consistently across the entire application.
 * Every composable that needs rounded corners should reference these tokens
 * via [MaterialTheme.shapes] rather than constructing ad-hoc shapes.
 *
 * Token mapping:
 *  - extraSmall → 8dp  (chips, small badges, pill indicators)
 *  - small      → 12dp (buttons, text fields, small cards)
 *  - medium     → 16dp (standard cards, dialogs, sheets)
 *  - large      → 24dp (prominent cards, progress sections)
 *  - extraLarge → 28dp (hero cards, full-screen overlays)
 */
val MediMedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
