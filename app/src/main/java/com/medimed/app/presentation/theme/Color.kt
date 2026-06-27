package com.medimed.app.presentation.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Palette ───────────────────────────────────────────────────────────
// These five colors define the entire visual identity of MediMed.
// Do not introduce additional branding colors.
val SoftApricot = Color(0xFFF9DBBD)
val CottonCandy = Color(0xFFFFA5AB)
val BlushRose = Color(0xFFDA627D)
val BerryCrush = Color(0xFFA53860)
val NightBordeaux = Color(0xFF450920)

// ─── Semantic Colors ─────────────────────────────────────────────────────────
// Purpose-driven aliases that communicate meaning rather than appearance.
// Use these in UI code instead of raw brand colors or arbitrary hex values.

/** Status: dose has been taken — accessible green that harmonizes with the warm palette */
val StatusTaken = Color(0xFF2D7A3E)

/** Status: dose has been skipped — muted, de-emphasized */
val StatusSkipped = Color(0xFF8C7680)

/** Inventory alert: low stock or out-of-stock warning accent */
val LowStockWarning = Color(0xFFCC6B2E)

// ─── Light Theme Surface Colors ──────────────────────────────────────────────
val LightBackground = Color(0xFFFCF6F0)       // Premium warm cream
val LightSurface = Color(0xFFFFFFFF)           // Clean white cards
val LightSurfaceVariant = Color(0xFFF7EDE2)    // Soft cream variant

// ─── Dark Theme Surface Colors ───────────────────────────────────────────────
val DarkBackground = Color(0xFF1C1316)         // Premium dark warm plum
val DarkSurface = Color(0xFF281E21)            // Warm dark cards
val DarkSurfaceVariant = Color(0xFF33252A)     // Dark plum variant
val DarkSecondaryContainer = Color(0xFF3A1A24) // Premium dark plum secondary

// ─── Error Colors ────────────────────────────────────────────────────────────
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color.White
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
