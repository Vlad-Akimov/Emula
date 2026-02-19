package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private var scannedTag = mutableStateOf<TagData?>(null)

    // Флаг для отслеживания режима эмуляции
    private var isEmulating = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate")

        reader = NfcReader(this)
        repository = TagRepository(this)
        emulator = TagEmulator(this)

        // Проверяем начальное состояние эмуляции
        isEmulating.value = emulator.isEmulating()

        Log.d("MainActivity", "Начальное состояние эмуляции: ${isEmulating.value}")

        setContent {
            NfcTagEmulatorTheme {
                if (showScanScreen.value) {
                    ScanScreen(
                        repository = repository,
                        scannedTag = scannedTag.value,
                        onBackClick = {
                            showScanScreen.value = false
                            scannedTag.value = null
                        }
                    )
                } else {
                    EmulatorMainScreen(
                        repository = repository,
                        emulator = emulator,
                        isEmulating = isEmulating.value,
                        onEmulationStateChanged = { newState ->
                            isEmulating.value = newState
                            updateNfcState()
                        },
                        onScanClick = {
                            showScanScreen.value = true
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume, isEmulating = ${isEmulating.value}")

        // Включаем чтение ТОЛЬКО если не эмулируем
        if (!isEmulating.value) {
            reader.enable(this)
            Log.d("MainActivity", "Режим чтения включен")
        } else {
            Log.d("MainActivity", "Режим эмуляции - чтение не включаем")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")

        // Всегда отключаем чтение при паузе
        reader.disable(this)
        Log.d("MainActivity", "Режим чтения отключен")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        Log.d("MainActivity", "onNewIntent, action = ${intent.action}")

        // ВАЖНО: Игнорируем NFC интенты во время эмуляции
        if (isEmulating.value) {
            Log.d("MainActivity", "Режим эмуляции - игнорируем сканирование")
            Toast.makeText(
                this,
                "Сейчас активен режим эмуляции. Остановите эмуляцию для сканирования.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val tagData = reader.readTag(intent)

        tagData?.let {
            Log.d("MainActivity", "Прочитана метка: ${it.uid}")

            // Сохраняем метку
            repository.saveTag(it)

            if (showScanScreen.value) {
                scannedTag.value = it
                Toast.makeText(
                    this,
                    "Метка отсканирована: ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Новая метка сохранена: ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateNfcState() {
        Log.d("MainActivity", "updateNfcState, isEmulating = ${isEmulating.value}")

        if (isEmulating.value) {
            // В режиме эмуляции - ПОЛНОСТЬЮ отключаем чтение
            reader.disable(this)
            Log.d("MainActivity", "Режим эмуляции: чтение отключено")

            Toast.makeText(
                this,
                "⚡ Режим эмуляции активен. Теперь телефон работает как метка.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // В режиме ожидания - включаем чтение
            reader.enable(this)
            Log.d("MainActivity", "Режим чтения: включено")

            Toast.makeText(
                this,
                "📡 Режим чтения активен. Поднесите NFC метку.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorMainScreen(
    repository: TagRepository,
    emulator: TagEmulator,
    isEmulating: Boolean,
    onEmulationStateChanged: (Boolean) -> Unit,
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
            // Верхняя панель
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Эмулятор NFC",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Статус
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isEmulating) Color.Green else Color.Yellow
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEmulating)
                                    "⚡ ЭМУЛЯЦИЯ МЕТКИ"
                                else
                                    "📡 РЕЖИМ ЧТЕНИЯ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isEmulating) Color.Green else Color.Yellow
                            )
                        }
                    }

                    // Кнопка сканирования (отключаем во время эмуляции)
                    FilledTonalButton(
                        onClick = onScanClick,
                        enabled = !isEmulating,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isEmulating)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            if (isEmulating) "ЭМУЛЮ" else "СКАНИТЬ",
                            fontSize = 12.sp
                        )
                    }
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
                        // Индикатор
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .scale(if (isEmulating) pulseScale else 1f)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            if (isEmulating)
                                                Color.Green
                                            else
                                                MaterialTheme.colorScheme.primary,
                                            if (isEmulating)
                                                Color.Green.copy(alpha = 0.3f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEmulating) "⚡" else "NFC",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
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
                                    text = if (isEmulating)
                                        "⚡ ЭМУЛЯЦИЯ АКТИВНА"
                                    else
                                        "НЕТ СОХРАНЕННЫХ",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isEmulating) Color.Green else Color.White
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isEmulating)
                                        "Телефон работает как NFC метка.\nПоднесите другой телефон для считывания."
                                    else
                                        "Нажмите СКАНИРОВАТЬ чтобы добавить метку",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (!isEmulating) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onScanClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("СКАНИРОВАТЬ МЕТКУ")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    emulator.setEmulatingTag(null)
                                    emulatingUid = null
                                    onEmulationStateChanged(false)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("ОСТАНОВИТЬ ЭМУЛЯЦИЮ")
                            }
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
                                    // Останавливаем эмуляцию
                                    emulator.setEmulatingTag(null)
                                    emulatingUid = null
                                    onEmulationStateChanged(false)
                                    Toast.makeText(
                                        context,
                                        "Эмуляция остановлена",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Запускаем эмуляцию
                                    emulator.setEmulatingTag(selectedTag)
                                    emulatingUid = selectedTag.uid
                                    onEmulationStateChanged(true)
                                    Toast.makeText(
                                        context,
                                        "⚡ Эмуляция: ${selectedTag.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
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

                                // Если это эмулируемая метка, обновляем
                                if (uid == emulatingUid) {
                                    val updatedTag = tags.find { it.uid == uid }
                                    updatedTag?.let {
                                        emulator.setEmulatingTag(it)
                                    }
                                }

                                Toast.makeText(context, "Имя обновлено", Toast.LENGTH_SHORT).show()
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
                                onEmulationStateChanged(false)
                            }

                            showDeleteDialog = false
                            Toast.makeText(context, "Метка удалена", Toast.LENGTH_SHORT).show()
                        }
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