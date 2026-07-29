package com.apptolast.fledge.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FledgeTeal,
    onPrimary = Color.White,
    primaryContainer = FledgeSurface2,
    onPrimaryContainer = FledgeInk,
    secondary = FledgeGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EEE8),
    onSecondaryContainer = FledgeInk,
    tertiary = FledgeGold,
    onTertiary = FledgeInk,
    background = FledgeMist,
    onBackground = FledgeInk,
    surface = Color.White,
    onSurface = FledgeInk,
    surfaceVariant = FledgeSurface2,
    onSurfaceVariant = FledgeMuted,
    outline = FledgeLine,
    outlineVariant = FledgeLine,
    error = FledgeRed,
    errorContainer = Color(0xFFF8DEDE),
    onErrorContainer = FledgeInk,
)

private val DarkColors = darkColorScheme(
    primary = FledgeTealDark,
    onPrimary = FledgeInk,
    primaryContainer = FledgeDarkSurface2,
    onPrimaryContainer = Color.White,
    secondary = FledgeGreenDark,
    onSecondary = FledgeInk,
    secondaryContainer = FledgeDarkSurface2,
    onSecondaryContainer = Color.White,
    tertiary = FledgeGold,
    onTertiary = FledgeInk,
    background = FledgeDark,
    onBackground = Color.White,
    surface = FledgeDarkSurface,
    onSurface = Color.White,
    surfaceVariant = FledgeDarkSurface2,
    onSurfaceVariant = FledgeFaint,
    outline = Color(0xFF3B4A45),
    outlineVariant = Color(0xFF3B4A45),
    error = FledgeCoral,
    errorContainer = Color(0xFF5A2626),
    onErrorContainer = Color.White,
)

@Composable
fun FledgeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FledgeTypography,
        content = content,
    )
}
