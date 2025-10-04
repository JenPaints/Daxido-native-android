package com.daxido.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DaxidoLightColorScheme = lightColorScheme(
    primary = DaxidoGold,
    onPrimary = DaxidoWhite,
    primaryContainer = DaxidoPaleGold,
    onPrimaryContainer = DaxidoDarkBrown,
    secondary = DaxidoMediumBrown,
    onSecondary = DaxidoWhite,
    secondaryContainer = DaxidoPaleGold,
    onSecondaryContainer = DaxidoDarkBrown,
    tertiary = DaxidoLightGold,
    onTertiary = DaxidoDarkBrown,
    tertiaryContainer = DaxidoPaleGold,
    onTertiaryContainer = DaxidoDarkBrown,
    background = DaxidoWhite,
    onBackground = DaxidoDarkBrown,
    surface = DaxidoWhite,
    onSurface = DaxidoDarkBrown,
    surfaceVariant = DaxidoLightGray,
    onSurfaceVariant = DaxidoMediumBrown,
    error = DaxidoError,
    onError = DaxidoWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = DaxidoMediumBrown.copy(alpha = 0.5f),
    outlineVariant = DaxidoMediumBrown.copy(alpha = 0.2f),
    scrim = DaxidoBlack.copy(alpha = 0.5f),
    inverseSurface = DaxidoDarkBrown,
    inverseOnSurface = DaxidoWhite,
    inversePrimary = DaxidoLightGold
)

private val DaxidoDarkColorScheme = darkColorScheme(
    primary = DaxidoGold,
    onPrimary = DaxidoDarkBrown,
    primaryContainer = DaxidoMediumBrown,
    onPrimaryContainer = DaxidoPaleGold,
    secondary = DaxidoLightGold,
    onSecondary = DaxidoDarkBrown,
    secondaryContainer = DaxidoMediumBrown,
    onSecondaryContainer = DaxidoPaleGold,
    tertiary = DaxidoPaleGold,
    onTertiary = DaxidoDarkBrown,
    tertiaryContainer = DaxidoMediumBrown,
    onTertiaryContainer = DaxidoPaleGold,
    background = Color(0xFF1A1A1A),
    onBackground = DaxidoPaleGold,
    surface = Color(0xFF2A2A2A),
    onSurface = DaxidoPaleGold,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = DaxidoLightGold,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DaxidoGold.copy(alpha = 0.5f),
    outlineVariant = DaxidoGold.copy(alpha = 0.2f),
    scrim = DaxidoBlack.copy(alpha = 0.7f),
    inverseSurface = DaxidoPaleGold,
    inverseOnSurface = DaxidoDarkBrown,
    inversePrimary = DaxidoMediumBrown
)

@Composable
fun DaxidoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DaxidoDarkColorScheme
        else -> DaxidoLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}