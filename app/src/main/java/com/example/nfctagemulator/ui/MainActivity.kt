package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.components.TagCard
import com.example.nfctagemulator.ui.screen.ScanScreen
import com.example.nfctagemulator.ui.theme.NfcTagEmulatorTheme

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var showScanScreen = mutableStateOf(false)

    // Добавляем состояние для передачи данных сканирования
    private var scannedTag = mutableStateOf<TagData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reader = NfcReader(this)
        repository = TagRepository(this)
        emulator = TagEmulator(this)

        setContent {
            NfcTagEmulatorTheme {
                if (showScanScreen.value) {
                    ScanScreen(
                        repository = repository,
                        scannedTag = scannedTag.value,
                        onBackClick = {
                            showScanScreen.value = false
                            scannedTag.value = null // Сбрасываем после закрытия
                        }
                    )
                } else {
                    EmulatorMainScreen(
                        repository = repository,
                        emulator = emulator,
                        onScanClick = { showScanScreen.value = true }
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
            // Форматируем UID для отображения
            val formattedUid = formatUid(it.uid)

            // Сохраняем метку
            repository.saveTag(it)

            // Если мы на экране сканирования, показываем уведомление
            if (showScanScreen.value) {
                scannedTag.value = it
                Toast.makeText(
                    this,
                    "Метка отсканирована: ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Если мы на главном экране, просто уведомляем
                Toast.makeText(
                    this,
                    "Новая метка сохранена: ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorMainScreen(
    repository: TagRepository,
    emulator: TagEmulator,
    onScanClick: () -> Unit
) {
    val context = LocalContext.current
    var tags by remember { mutableStateOf(repository.getAllTags()) }
    var emulatingUid by remember { mutableStateOf(emulator.getEmulatingTagUid()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTagForRename by remember { mutableStateOf<TagData?>(null) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<TagData?>(null) }

    // Обновляем список при возврате на экран
    LaunchedEffect(Unit) {
        tags = repository.getAllTags()
        emulatingUid = emulator.getEmulatingTagUid()
    }

    // Анимация для активной эмуляции
    val infiniteTransition = rememberInfiniteTransition(label = "emulation_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
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
                .padding(16.dp)
        ) {
            // Верхняя панель с заголовком и кнопкой сканирования
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Эмулятор NFC",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (emulatingUid != null) {
                        Text(
                            text = "⚡ Активная эмуляция",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Кнопка сканирования
                FilledTonalButton(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Сканировать")
                }
            }

            if (tags.isEmpty()) {
                // Пустое состояние
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Анимированный индикатор
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .scale(pulseScale)
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

                        Text(
                            text = "Нет сохраненных меток",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Нажмите 'Сканировать' чтобы добавить первую метку",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onScanClick,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Сканировать метку")
                        }
                    }
                }
            } else {
                // Список сохраненных меток
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tags) { tag ->
                        TagCard(
                            tag = tag,
                            isEmulating = tag.uid == emulatingUid,
                            onEmulateClick = { selectedTag ->
                                if (selectedTag.uid == emulatingUid) {
                                    emulator.setEmulatingTag(null)
                                    emulatingUid = null
                                    Toast.makeText(context, "Эмуляция остановлена", Toast.LENGTH_SHORT).show()
                                } else {
                                    emulator.setEmulatingTag(selectedTag)
                                    emulatingUid = selectedTag.uid
                                    Toast.makeText(context, "Эмуляция: ${selectedTag.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRenameClick = { tagToRename ->
                                selectedTagForRename = tagToRename
                                newName = tagToRename.name
                                showRenameDialog = true
                            },
                            onDeleteClick = { tag ->
                                tagToDelete = tag
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог переименования
    if (showRenameDialog && selectedTagForRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать метку") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Имя метки") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            selectedTagForRename?.uid?.let { uid ->
                                repository.updateTagName(uid, newName)
                                tags = repository.getAllTags()
                            }
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог удаления
    if (showDeleteDialog && tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить метку?") },
            text = { Text("Вы уверены, что хотите удалить метку \"${tagToDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tagToDelete?.uid?.let { uid ->
                            repository.deleteTag(uid)
                            tags = repository.getAllTags()

                            if (uid == emulatingUid) {
                                emulator.setEmulatingTag(null)
                                emulatingUid = null
                            }
                        }
                        showDeleteDialog = false
                        Toast.makeText(context, "Метка удалена", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}