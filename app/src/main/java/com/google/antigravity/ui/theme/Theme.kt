package com.google.antigravity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AgPrimary,
    secondary = AgSecondary,
    background = AgDarkBackground,
    surface = AgSurface,
    surfaceVariant = AgSurfaceVariant,
    onPrimary = AgTextPrimary,
    onSecondary = AgDarkBackground,
    onBackground = AgTextPrimary,
    onSurface = AgTextPrimary,
    error = AgError
)

@Composable
fun AntigravityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
