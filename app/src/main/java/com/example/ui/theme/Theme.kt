package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = SageTealBase, // #579F92
    onPrimary = Color.Black,
    primaryContainer = SageTeal800,
    onPrimaryContainer = SageTeal100,
    secondary = SageTeal400,
    onSecondary = Color.Black,
    secondaryContainer = SageTeal700,
    onSecondaryContainer = SageTeal100,
    tertiary = WarningAmber,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = SageTealBase, // #579F92
    onPrimary = Color.White,
    primaryContainer = SageTeal100,
    onPrimaryContainer = SageTeal900,
    secondary = SageTeal700,
    onSecondary = Color.White,
    secondaryContainer = SageTeal100,
    onSecondaryContainer = SageTeal800,
    tertiary = WarningAmber,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight.copy(alpha = 0.6f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded financial styling consistent
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
