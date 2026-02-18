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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository

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

    // Обновляем UI при сканировании новой метки
    LaunchedEffect(scannedTag) {
        scannedTag?.let { tag ->
            lastScannedUid = tag.uid
            scanCount++

            Toast.makeText(
                context,
                "Метка сохранена: ${tag.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Анимация сканирования
    val infiniteTransition = rememberInfiniteTransition(label = "scan_animation")
    val scanScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Сканирование NFC меток") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A12),
                            Color(0xFF1A1A2F)
                        )
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
                // Анимированная иконка сканирования
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .scale(scanScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NFC",
                        fontSize = 48.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Статус сканирования
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lastScannedUid != null)
                                "Последний UID: ${formatUid(lastScannedUid)}"
                            else
                                "Ожидание метки...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Поднесите NFC метку к задней панели телефона",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (scanCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Отсканировано меток: $scanCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Кнопка возврата
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Вернуться к эмуляции")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Инструкция
                Text(
                    text = "Метки автоматически сохраняются при сканировании",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
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
    } ?: "Неизвестно"
}