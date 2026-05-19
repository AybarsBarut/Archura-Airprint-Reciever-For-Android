package com.archura.airprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Paper,
    secondary = Signal,
    onSecondary = Paper,
    background = Mist,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SoftLine,
    outline = SoftLine,
)

@Composable
fun AirPrintTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AirPrintTypography,
        content = content,
    )
}
