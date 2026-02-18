package com.example.nfctagemulator.ui.theme

import android.app.Activity
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

// Неоновая цветовая палитра для тёмной темы
val NeonDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Яркий циан
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF004D66),
    onPrimaryContainer = Color(0xFFB3ECFF),

    secondary = Color(0xFFAA00FF), // Фиолетовый неон
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF4A0072),
    onSecondaryContainer = Color(0xFFE6B3FF),

    tertiary = Color(0xFFFF00AA), // Розовый неон
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF660044),
    onTertiaryContainer = Color(0xFFFFB3E6),

    background = Color(0xFF0A0A12), // Глубокий тёмно-синий
    onBackground = Color(0xFFE6E6FF),

    surface = Color(0xFF12121F), // Чуть светлее фона
    onSurface = Color(0xFFE6E6FF),
    surfaceVariant = Color(0xFF1E1E30),
    onSurfaceVariant = Color(0xFFB0B0D0),

    error = Color(0xFFFF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF660000),
    onErrorContainer = Color(0xFFFFB3B3),

    outline = Color(0xFF2E2E4A),
    outlineVariant = Color(0xFF3D3D5C),

    inverseSurface = Color(0xFFE6E6FF),
    inverseOnSurface = Color(0xFF0A0A12),
    inversePrimary = Color(0xFF006680)
)

@Composable
fun NfcTagEmulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Отключаем динамические цвета для сохранения неона
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NeonDarkColorScheme
        else -> lightColorScheme() // Светлую тему можно настроить позже при необходимости
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}