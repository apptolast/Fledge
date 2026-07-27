package com.apptolast.fledge.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = FledgeGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = FledgeSky,
    tertiary = FledgeClay,
    background = FledgeMist,
    onBackground = FledgeInk,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = FledgeInk,
)

private val DarkColors = darkColorScheme(
    primary = FledgeGreenDark,
    onPrimary = FledgeInk,
    secondary = FledgeSky,
    tertiary = FledgeClay,
    background = FledgeDarkBackground,
    surface = androidx.compose.ui.graphics.Color(0xFF121815),
)

@Composable
fun FledgeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FledgeTypography,
        content = content,
    )
}
