package com.example.nfctagemulator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

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

// Adaptive dimensions based on screen size
@Composable
fun getAdaptiveDimens(): AdaptiveDimens {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return when {
        screenWidthDp < 360 -> SmallDimens()      // Very small phones
        screenWidthDp < 480 -> MediumDimens()     // Regular phones
        screenWidthDp < 720 -> LargeDimens()      // Large phones / small tablets
        else -> TabletDimens()                    // Tablets
    }
}

interface AdaptiveDimens {
    val paddingSmall: Int
    val paddingMedium: Int
    val paddingLarge: Int
    val cardCornerRadius: Int
    val buttonHeight: Int
    val iconSize: Int
    val headerFontSize: Int
    val titleFontSize: Int
    val bodyFontSize: Int
    val scannerSize: Int
}

class SmallDimens : AdaptiveDimens {
    override val paddingSmall = 8
    override val paddingMedium = 12
    override val paddingLarge = 16
    override val cardCornerRadius = 12
    override val buttonHeight = 44
    override val iconSize = 20
    override val headerFontSize = 18
    override val titleFontSize = 14
    override val bodyFontSize = 12
    override val scannerSize = 160
}

class MediumDimens : AdaptiveDimens {
    override val paddingSmall = 12
    override val paddingMedium = 16
    override val paddingLarge = 20
    override val cardCornerRadius = 16
    override val buttonHeight = 48
    override val iconSize = 24
    override val headerFontSize = 22
    override val titleFontSize = 16
    override val bodyFontSize = 14
    override val scannerSize = 200
}

class LargeDimens : AdaptiveDimens {
    override val paddingSmall = 16
    override val paddingMedium = 24
    override val paddingLarge = 32
    override val cardCornerRadius = 20
    override val buttonHeight = 52
    override val iconSize = 28
    override val headerFontSize = 28
    override val titleFontSize = 18
    override val bodyFontSize = 16
    override val scannerSize = 260
}

class TabletDimens : AdaptiveDimens {
    override val paddingSmall = 24
    override val paddingMedium = 32
    override val paddingLarge = 48
    override val cardCornerRadius = 24
    override val buttonHeight = 56
    override val iconSize = 32
    override val headerFontSize = 36
    override val titleFontSize = 22
    override val bodyFontSize = 18
    override val scannerSize = 320
}

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
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}