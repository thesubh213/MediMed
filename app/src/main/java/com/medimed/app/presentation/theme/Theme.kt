package com.medimed.app.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = BerryCrush,
    onPrimary = Color.White,
    primaryContainer = SoftApricot,
    onPrimaryContainer = NightBordeaux,
    secondary = BlushRose,
    onSecondary = Color.White,
    secondaryContainer = CottonCandy,
    onSecondaryContainer = NightBordeaux,
    background = LightBackground,
    onBackground = NightBordeaux,
    surface = LightSurface,
    onSurface = NightBordeaux,
    surfaceVariant = SoftApricot,      
    onSurfaceVariant = NightBordeaux,
    outline = BlushRose,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = CottonCandy,
    onPrimary = NightBordeaux,
    primaryContainer = BerryCrush,
    onPrimaryContainer = Color.White,
    secondary = BlushRose,
    onSecondary = Color.White,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = SoftApricot,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SoftApricot,
    outline = BlushRose,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun MediMedTheme(
    darkTheme: Boolean = false, 
    content: @Composable () -> Unit
) {
    
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surfaceVariant.toArgb()

                
                val isDark = false
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDimens provides MediMedDimens()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MediMedTypography,
            shapes = MediMedShapes,
            content = content
        )
    }
}
