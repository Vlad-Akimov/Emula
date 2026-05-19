package com.example.nfctagemulator.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.ui.components.GlowCard
import com.example.nfctagemulator.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    repository: TagRepository,
    scannedTag: TagData?,
    onNavigateToCreate: () -> Unit
) {
    val context = LocalContext.current
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var lastScannedUid by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var localScannedTag by remember { mutableStateOf(scannedTag) }

    LaunchedEffect(scannedTag) {
        scannedTag?.let { tag ->
            if (tag.uid != lastScannedUid) {
                lastScannedUid = tag.uid
                localScannedTag = tag
                isScanning = false

                Toast.makeText(context, "✅ ${tag.name}", Toast.LENGTH_SHORT).show()

                delay(2000)
                isScanning = true
                localScannedTag = null
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "iconScale"
    )
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "glow"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "breath"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(BackgroundDark, SurfaceDark)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(dimens.paddingLarge.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "SCAN NFC",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = NeonCyan,
                fontSize = dimens.headerFontSize.sp,
                modifier = Modifier.padding(bottom = if (isLandscape) dimens.paddingMedium.dp else dimens.paddingLarge.dp)
            )

            // Scanner animation
            val scannerSize = if (isLandscape) dimens.scannerSize * 0.7f else dimens.scannerSize.toFloat()

            Box(
                modifier = Modifier
                    .size(scannerSize.dp)
                    .drawBehind {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 2

                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.4f), NeonPurple.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.4f)),
                                center = Offset(centerX, centerY)
                            ),
                            startAngle = rotationAngle,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2f)
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(centerX, centerY),
                                radius = radius * 0.85f
                            ),
                            radius = radius * 0.85f,
                            style = Stroke(width = 1.5f)
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = if (localScannedTag != null)
                                    listOf(NeonGreen.copy(alpha = glowIntensity * 0.8f), NeonCyan.copy(alpha = 0.1f), Color.Transparent)
                                else
                                    listOf(NeonCyan.copy(alpha = glowIntensity), NeonPurple.copy(alpha = 0.2f), Color.Transparent),
                                center = Offset(centerX, centerY),
                                radius = radius * 0.6f
                            ),
                            radius = radius * 0.6f
                        )

                        drawCircle(
                            color = if (localScannedTag != null) NeonGreen else NeonCyan,
                            radius = 4f,
                            center = Offset(centerX, centerY)
                        )
                    }
                    .scale(if (isScanning) breathScale else 1f)
                    .clip(RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (localScannedTag != null) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f)
                    ) {
                        Text(
                            text = "✓",
                            fontSize = (scannerSize / 3).sp,
                            color = NeonGreen,
                            modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(32.dp), clip = false)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC",
                        modifier = Modifier
                            .size((scannerSize / 2.5f).dp)
                            .scale(iconScale)
                            .graphicsLayer { alpha = 0.9f },
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) dimens.paddingMedium.dp else dimens.paddingLarge.dp))

            // Status Card
            GlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) dimens.paddingMedium.dp else 0.dp),
                gradientColors = if (localScannedTag != null)
                    listOf(NeonGreen.copy(alpha = 0.08f), SurfaceDark)
                else
                    listOf(NeonCyan.copy(alpha = 0.08f), SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.paddingMedium.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = dimens.paddingMedium.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when {
                                        localScannedTag != null -> NeonGreen
                                        isScanning -> NeonCyan
                                        else -> NeonPurple
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                localScannedTag != null -> "TAG DETECTED"
                                isScanning -> "SCANNING"
                                else -> "READY"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                            color = when {
                                localScannedTag != null -> NeonGreen
                                isScanning -> NeonCyan
                                else -> NeonPurple
                            },
                            fontSize = if (isLandscape) (dimens.bodyFontSize - 2).sp else dimens.bodyFontSize.sp
                        )
                    }

                    if (localScannedTag != null) {
                        Text(
                            text = localScannedTag!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontSize = if (isLandscape) dimens.titleFontSize.sp else (dimens.titleFontSize + 4).sp,
                            modifier = Modifier.padding(bottom = dimens.paddingSmall.dp)
                        )
                        Text(
                            text = formatUid(localScannedTag!!.uid),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = NeonCyan.copy(alpha = 0.8f),
                            fontSize = (dimens.bodyFontSize - 1).sp,
                            modifier = Modifier.padding(bottom = dimens.paddingSmall.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonPurple.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = dimens.paddingSmall.dp / 2)
                        ) {
                            Text(
                                text = localScannedTag!!.type.name.replace("NDEF_", ""),
                                fontSize = if (isLandscape) (dimens.bodyFontSize - 4).sp else (dimens.bodyFontSize - 2).sp,
                                color = NeonPurple,
                                modifier = Modifier.padding(horizontal = dimens.paddingSmall.dp, vertical = dimens.paddingSmall.dp / 2),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = if (isScanning)
                                if (isLandscape) "Position an NFC tag\nnear the device" else "Position an NFC tag near the device"
                            else
                                "Ready for next scan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = dimens.bodyFontSize.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatUid(uid: String?): String {
    return uid?.let {
        if (it.length > 8) {
            it.chunked(2).joinToString(":").uppercase()
        } else {
            it.uppercase()
        }
    } ?: "Unknown"
}
