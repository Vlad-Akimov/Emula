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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    repository: TagRepository,
    scannedTag: TagData?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var lastScannedUid by remember { mutableStateOf<String?>(null) }
    var scanCount by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(scannedTag) {
        scannedTag?.let { tag ->
            lastScannedUid = tag.uid
            scanCount++
            isScanning = false

            Toast.makeText(
                context,
                "✅ ${tag.name}",
                Toast.LENGTH_SHORT
            ).show()

            kotlinx.coroutines.delay(2000)
            isScanning = true
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

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SCAN NFC",
                        color = NeonCyan
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 20.sp, color = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                // Animated scanner
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(if (isScanning) pulseScale else 1f)
                        .drawBehind {
                            // Scanning arc - рисуем вручную без Stroke
                            val arcSize = size.minDimension * 0.8f
                            val arcOffset = (size.minDimension - arcSize) / 2

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

                            // Center circle
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = if (scannedTag != null)
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
                        text = if (scannedTag != null) "✓" else "NFC",
                        fontSize = 48.sp,
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
                                scannedTag != null -> "TAG DETECTED"
                                isScanning -> "SCANNING"
                                else -> "READY"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = when {
                                scannedTag != null -> NeonGreen
                                isScanning -> NeonCyan
                                else -> NeonPurple
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (scannedTag != null) {
                            Text(
                                text = scannedTag.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = formatUid(scannedTag.uid),
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

                Spacer(modifier = Modifier.height(48.dp))

                // Back button
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("BACK TO EMULATOR")
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