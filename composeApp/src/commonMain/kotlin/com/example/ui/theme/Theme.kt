package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TintColorDark,
    background = BackgroundDark,
    surface = BackgroundDark,
    onPrimary = BackgroundDark, // typically contrasting color
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = IconDark
)

private val LightColorScheme = lightColorScheme(
    primary = TintColorLight,
    background = BackgroundLight,
    surface = BackgroundLight,
    onPrimary = BackgroundLight,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = IconLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Default typography is fine for now
        content = content
    )
}
