package com.medimed.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Immutable
data class MediMedDimens(
    
    
    val spacingXs: Dp = 4.dp,
    
    val spacingSm: Dp = 8.dp,
    
    val spacingMd: Dp = 12.dp,
    
    val spacingLg: Dp = 16.dp,
    
    val spacingXl: Dp = 20.dp,
    
    val spacingXxl: Dp = 24.dp,
    
    val spacingXxxl: Dp = 32.dp,

    
    
    val elevationNone: Dp = 0.dp,
    
    val elevationLow: Dp = 2.dp,
    
    val elevationMedium: Dp = 4.dp,
    
    val elevationHigh: Dp = 6.dp,

    
    
    val minTouchTarget: Dp = 48.dp,
    
    val largeTouchTarget: Dp = 56.dp,
    
    val xlTouchTarget: Dp = 64.dp,

    
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 18.dp,
    val iconLg: Dp = 24.dp,
    val iconXl: Dp = 32.dp,

    
    val borderThin: Dp = 1.dp,
    val borderMedium: Dp = 1.5.dp,
    val borderThick: Dp = 2.dp,
    val borderAccent: Dp = 4.dp,

    
    val progressHeight: Dp = 10.dp
)


val LocalDimens = staticCompositionLocalOf { MediMedDimens() }
