package com.yin_radio.yin_radio_android_app.ui.theme

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
import com.yin_radio.yin_radio_android_app.data.local.prefs.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentDark,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceVariant = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentDark,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceVariant = LightSurface
)

@Composable
fun YinRadioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Depending on Theme from Persistent DataStore, set Theme
    // Default to System Theme if none provided/ set
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Render UI App Theme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
