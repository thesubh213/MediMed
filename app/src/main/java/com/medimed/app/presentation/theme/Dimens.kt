package com.medimed.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MediMed Dimension Tokens
 *
 * Centralized spacing, elevation, and sizing values that ensure visual consistency
 * across the entire application. All composables should reference these tokens
 * via [LocalDimens.current] rather than using hardcoded dp values.
 *
 * Usage:
 * ```
 * val dimens = LocalDimens.current
 * Modifier.padding(dimens.spacingMd)
 * ```
 */
@Immutable
data class MediMedDimens(
    // ─── Spacing Scale ───────────────────────────────────────────────────
    /** 4dp — Tightest spacing: icon-to-text gaps, inline element separation */
    val spacingXs: Dp = 4.dp,
    /** 8dp — Small spacing: intra-component gaps, list item internal padding */
    val spacingSm: Dp = 8.dp,
    /** 12dp — Medium spacing: card internal padding, form field gaps */
    val spacingMd: Dp = 12.dp,
    /** 16dp — Standard spacing: section gaps, card outer padding, list content padding */
    val spacingLg: Dp = 16.dp,
    /** 20dp — Large spacing: screen horizontal padding, major section dividers */
    val spacingXl: Dp = 20.dp,
    /** 24dp — Extra large: prominent section padding, hero area insets */
    val spacingXxl: Dp = 24.dp,
    /** 32dp — Maximum: screen-level vertical separation, CTA section padding */
    val spacingXxxl: Dp = 32.dp,

    // ─── Elevation Scale ─────────────────────────────────────────────────
    /** 0dp — Flat surfaces: backgrounds, sheets flush with parent */
    val elevationNone: Dp = 0.dp,
    /** 2dp — Subtle lift: standard cards, list items */
    val elevationLow: Dp = 2.dp,
    /** 4dp — Moderate lift: image containers, interactive cards */
    val elevationMedium: Dp = 4.dp,
    /** 6dp — High prominence: hero cards, reminder overlay */
    val elevationHigh: Dp = 6.dp,

    // ─── Component Sizes ─────────────────────────────────────────────────
    /** Minimum touch target per Android accessibility guidelines */
    val minTouchTarget: Dp = 48.dp,
    /** Large touch target for primary actions and elderly-friendly UI */
    val largeTouchTarget: Dp = 56.dp,
    /** Extra large touch target: FAB, critical action buttons */
    val xlTouchTarget: Dp = 64.dp,

    // ─── Icon Sizes ──────────────────────────────────────────────────────
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 18.dp,
    val iconLg: Dp = 24.dp,
    val iconXl: Dp = 32.dp,

    // ─── Border Widths ───────────────────────────────────────────────────
    val borderThin: Dp = 1.dp,
    val borderMedium: Dp = 1.5.dp,
    val borderThick: Dp = 2.dp,
    val borderAccent: Dp = 4.dp,

    // ─── Progress & Indicator ────────────────────────────────────────────
    val progressHeight: Dp = 10.dp
)

/**
 * CompositionLocal providing [MediMedDimens] throughout the composition tree.
 * Access via `LocalDimens.current` inside any composable.
 */
val LocalDimens = staticCompositionLocalOf { MediMedDimens() }
