package com.aqsama.neomarkor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = NeoGreen,
    onPrimary = NeoSurface,
    primaryContainer = NeoGreenContainer,
    onPrimaryContainer = NeoInk,
    secondary = NeoGreenLight,
    onSecondary = NeoInk,
    background = NeoSurface,
    onBackground = NeoOnSurface,
    surface = NeoSurface,
    onSurface = NeoOnSurface,
    outline = NeoOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = NeoGreenDark,
    onPrimary = NeoInkDark,
    primaryContainer = NeoGreenContainerDark,
    onPrimaryContainer = NeoInkDark,
    secondary = NeoGreenDark,
    onSecondary = NeoInk,
    background = NeoSurfaceDark,
    onBackground = NeoOnSurfaceDark,
    surface = NeoSurfaceDark,
    onSurface = NeoOnSurfaceDark,
    outline = NeoOutlineDark,
)

@Composable
fun NeoMarkorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeoMarkorTypography,
        content = content
    )
}
