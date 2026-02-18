package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.screen.SavedTagsScreen
import com.example.nfctagemulator.ui.theme.NfcTagEmulatorTheme

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private var tagIdText = mutableStateOf("Поднеси NFC метку")
    private var showSavedTags = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reader = NfcReader(this)
        repository = TagRepository(this)

        setContent {
            NfcTagEmulatorTheme {
                if (showSavedTags.value) {
                    SavedTagsScreen(
                        repository = repository,
                        onBackClick = { showSavedTags.value = false }
                    )
                } else {
                    MainScreenContent(
                        tagText = tagIdText.value,
                        onSavedTagsClick = { showSavedTags.value = true },
                        tagsCount = repository.getAllTags().size
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reader.enable(this)
    }

    override fun onPause() {
        super.onPause()
        reader.disable(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tagData: TagData? = reader.readTag(intent)

        tagData?.let {
            tagIdText.value = "UID: ${formatUid(it.uid)}"

            // Сохраняем метку
            repository.saveTag(it)

            Toast.makeText(
                this,
                "Метка сохранена: ${it.name}",
                Toast.LENGTH_SHORT
            ).show()
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
}

@Composable
fun MainScreenContent(
    tagText: String,
    onSavedTagsClick: () -> Unit,
    tagsCount: Int
) {
    // Анимация пульсации
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A12),
                        Color(0xFF1A1A2F)
                    )
                )
            )
    ) {
        // Основной контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Заглушка для иконки - просто декоративный элемент
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NFC",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Текст с UID метки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Инструкция
            Text(
                text = "Поднесите NFC метку к задней панели телефона",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопка перехода к сохраненным меткам
            Button(
                onClick = onSavedTagsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text(
                    text = "Сохраненные метки ($tagsCount)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Индикатор статуса NFC
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Green.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NFC готов к работе",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}