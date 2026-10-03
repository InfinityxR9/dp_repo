package com.naresh.lungsdemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,

    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimaryDark,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightPrimaryContainer,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,

    error = Error,
    onError = LightSurface
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,

    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkPrimary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkPrimaryContainer,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,

    error = Error,
    onError = DarkTextPrimary
)

@Composable
fun LungsdemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}