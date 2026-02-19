package com.example.nfctagemulator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val NeonDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyan.copy(alpha = 0.2f),
    onPrimaryContainer = NeonCyan,

    secondary = NeonPurple,
    onSecondary = Color.Black,
    secondaryContainer = NeonPurple.copy(alpha = 0.2f),
    onSecondaryContainer = NeonPurple,

    tertiary = NeonPink,
    onTertiary = Color.Black,
    tertiaryContainer = NeonPink.copy(alpha = 0.2f),
    onTertiaryContainer = NeonPink,

    background = BackgroundDark,
    onBackground = Color.White,

    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),

    error = Color(0xFFFF4444),
    onError = Color.Black,
    errorContainer = Color(0x66FF4444),
    onErrorContainer = Color(0xFFFFAAAA),

    outline = NeonCyan.copy(alpha = 0.3f),
    outlineVariant = NeonPurple.copy(alpha = 0.3f)
)

@Composable
fun NfcTagEmulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NeonDarkColorScheme
        else -> lightColorScheme() // Для светлой темы можно добавить позже
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}