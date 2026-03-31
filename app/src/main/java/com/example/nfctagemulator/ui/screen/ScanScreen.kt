package com.example.nfctagemulator.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
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
    var scanCount by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(true) }
    var localScannedTag by remember { mutableStateOf(scannedTag) }

    LaunchedEffect(scannedTag) {
        scannedTag?.let { tag ->
            if (tag.uid != lastScannedUid) {
                lastScannedUid = tag.uid
                localScannedTag = tag
                scanCount++
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
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
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
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Animated scanner
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(if (isScanning) pulseScale else 1f)
                    .drawBehind {
                        val arcSize = size.minDimension * 0.8f

                        for (i in 0..3) {
                            val startAngle = scanProgress * 360f + i * 90f
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(NeonCyan, NeonPurple, NeonPink, NeonCyan),
                                    center = Offset(size.width / 2, size.height / 2)
                                ),
                                startAngle = startAngle,
                                sweepAngle = 45f,
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = if (localScannedTag != null)
                                    listOf(NeonGreen, NeonCyan)
                                else
                                    listOf(NeonCyan, NeonPurple)
                            ),
                            radius = size.minDimension / 3
                        )
                    }
                    .clip(RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (localScannedTag != null) "✓" else "NFC",
                    fontSize = 52.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            localScannedTag != null -> "TAG DETECTED"
                            isScanning -> "SCANNING"
                            else -> "READY"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            localScannedTag != null -> NeonGreen
                            isScanning -> NeonCyan
                            else -> NeonPurple
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (localScannedTag != null) {
                        Text(
                            text = localScannedTag!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = formatUid(localScannedTag!!.uid),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeonCyan,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = if (isScanning)
                                "Bring an NFC tag close to your device"
                            else
                                "Tag saved successfully",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (scanCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scanned: $scanCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonPurple
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