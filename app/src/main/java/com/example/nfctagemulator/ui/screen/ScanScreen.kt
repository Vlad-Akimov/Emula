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
    var lastScannedUid by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var localScannedTag by remember { mutableStateOf(scannedTag) }

    LaunchedEffect(scannedTag) {
        scannedTag?.let { tag ->
            if (tag.uid != lastScannedUid) {
                lastScannedUid = tag.uid
                localScannedTag = tag
                isScanning = false

                Toast.makeText(
                    context,
                    "✅ ${tag.name}",
                    Toast.LENGTH_SHORT
                ).show()

                delay(2000)
                isScanning = true
                localScannedTag = null
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    // Smooth rotation for the outer ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Subtle pulse for the icon
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Gentle glow intensity
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Breathing effect for the inner circle
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "SCAN NFC",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Elegant scanner animation
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .drawBehind {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 2

                        // Outer ring - smooth rotating gradient
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.4f),
                                    NeonPurple.copy(alpha = 0.4f),
                                    NeonCyan.copy(alpha = 0.4f)
                                ),
                                center = Offset(centerX, centerY)
                            ),
                            startAngle = rotationAngle,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2f)
                        )

                        // Middle ring - static with gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = radius * 0.85f
                            ),
                            radius = radius * 0.85f,
                            style = Stroke(width = 1.5f)
                        )

                        // Inner glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = if (localScannedTag != null)
                                    listOf(
                                        NeonGreen.copy(alpha = glowIntensity * 0.8f),
                                        NeonCyan.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                else
                                    listOf(
                                        NeonCyan.copy(alpha = glowIntensity),
                                        NeonPurple.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                center = Offset(centerX, centerY),
                                radius = radius * 0.6f
                            ),
                            radius = radius * 0.6f
                        )

                        // Center dot
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
                // NFC Icon
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (localScannedTag != null) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f)
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 56.sp,
                                color = NeonGreen,
                                modifier = Modifier.shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    clip = false
                                )
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            modifier = Modifier
                                .size(80.dp)
                                .scale(iconScale)
                                .graphicsLayer {
                                    alpha = 0.9f
                                },
                            tint = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Status Card
            GlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                gradientColors = if (localScannedTag != null)
                    listOf(NeonGreen.copy(alpha = 0.08f), SurfaceDark)
                else
                    listOf(NeonCyan.copy(alpha = 0.08f), SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // Status dot
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
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = when {
                                localScannedTag != null -> NeonGreen
                                isScanning -> NeonCyan
                                else -> NeonPurple
                            }
                        )
                    }

                    if (localScannedTag != null) {
                        Text(
                            text = localScannedTag!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = formatUid(localScannedTag!!.uid),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = NeonCyan.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Tag type
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonPurple.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = localScannedTag!!.type.name.replace("NDEF_", ""),
                                fontSize = 10.sp,
                                color = NeonPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = if (isScanning)
                                "Position an NFC tag near the device"
                            else
                                "Ready for next scan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
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