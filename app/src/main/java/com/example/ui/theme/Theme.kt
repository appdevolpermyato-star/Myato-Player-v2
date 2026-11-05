package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StreetCultureDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = White,
    primaryContainer = PurpleIntense,
    onPrimaryContainer = White,
    secondary = PurpleBright,
    onSecondary = White,
    secondaryContainer = GraphiteDark,
    onSecondaryContainer = PurplePrimary,
    tertiary = PurplePrimary,
    onTertiary = BlackPrimary,
    background = BlackPrimary,
    onBackground = White,
    surface = BlackSecondary,
    onSurface = White,
    surfaceVariant = GraphiteDark,
    onSurfaceVariant = GrayMuted,
    outline = PurpleBorder,
    outlineVariant = GraphiteBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = StreetCultureDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
