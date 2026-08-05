package com.example.next.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom colors that don't fit into MaterialTheme.colorScheme slots
 * (category backgrounds, star ratings, shadows, etc.)
 */
data class CustomColors(
    val textHint: Color,
    val divider: Color,
    val shadow: Color,
    val iconInactive: Color,
    val starFilled: Color,
    val starEmpty: Color,
    val phonesBg: Color,
    val laptopsBg: Color,
    val camerasBg: Color,
    val accessoriesBg: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    // Explicit surface/background for screens that use White directly
    val surfaceWhite: Color,
    val textOnPrimary: Color,
)

val LightCustomColors = CustomColors(
    textHint = TextHint,
    divider = Divider,
    shadow = Shadow,
    iconInactive = IconInactive,
    starFilled = StarFilled,
    starEmpty = StarEmpty,
    phonesBg = PhonesBg,
    laptopsBg = LaptopsBg,
    camerasBg = CamerasBg,
    accessoriesBg = AccessoriesBg,
    primaryLight = PrimaryLight,
    primaryDark = PrimaryDark,
    surfaceWhite = White,
    textOnPrimary = TextOnPrimary,
)

val DarkCustomColors = CustomColors(
    textHint = DarkTextHint,
    divider = DarkDivider,
    shadow = DarkShadow,
    iconInactive = DarkIconInactive,
    starFilled = StarFilled,
    starEmpty = DarkStarEmpty,
    phonesBg = DarkPhonesBg,
    laptopsBg = DarkLaptopsBg,
    camerasBg = DarkCamerasBg,
    accessoriesBg = DarkAccessoriesBg,
    primaryLight = DarkPrimaryLight,
    primaryDark = PrimaryDark,
    surfaceWhite = DarkSurface,
    textOnPrimary = DarkTextOnPrimary,
)

val LocalCustomColors = compositionLocalOf { LightCustomColors }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Primary,
    onSecondary = TextOnPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Divider,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = DarkPrimaryLight,
    onPrimaryContainer = PrimaryLight,
    secondary = Primary,
    onSecondary = TextOnPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surfaceVariant = DarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
)

@Composable
fun NextTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    // Use Material3 dynamic colors on Android 12+ when following system
    val useDynamicColors = themeMode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = remember(darkTheme, useDynamicColors) {
        when {
            useDynamicColors && darkTheme -> dynamicDarkColorScheme(context)
            useDynamicColors && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val customColors = remember(darkTheme) {
        if (darkTheme) DarkCustomColors else LightCustomColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
