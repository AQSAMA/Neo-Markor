package com.aqsama.neomarkor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

/**
 * @param themeMode 0 = System, 1 = Light, 2 = Dark
 * @param accentColorArgb Custom accent color ARGB int, or 0 for default palette.
 */
@Composable
fun NeoMarkorTheme(
    themeMode: Int = 0,
    dynamicColor: Boolean = true,
    accentColorArgb: Int = 0,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.let { scheme ->
        if (accentColorArgb != 0) {
            val accent = Color(accentColorArgb)
            scheme.copy(primary = accent, primaryContainer = accent.copy(alpha = 0.2f))
        } else scheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeoMarkorTypography,
        content = content,
    )
}
