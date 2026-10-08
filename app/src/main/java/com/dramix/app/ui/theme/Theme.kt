package com.dramix.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonPlay,
    onPrimary = Color.White,
    primaryContainer = CrimsonPressed,
    onPrimaryContainer = Color.White,
    secondary = MidnightBorder,
    onSecondary = Slate50,
    background = PureBlack,
    onBackground = Slate50,
    surface = MidnightBase,
    onSurface = Slate50,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = Slate400,
    outline = MidnightBorder,
    error = RoseError,
    onError = Color.White
)

@Composable
fun DramixTheme(
    darkTheme: Boolean = true, // Always OLED dark
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            @Suppress("DEPRECATION")
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = PureBlack.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = PureBlack.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DramixTypography,
        shapes = DramixShapes,
        content = content
    )
}
