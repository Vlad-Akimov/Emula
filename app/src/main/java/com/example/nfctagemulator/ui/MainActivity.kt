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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
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
import com.example.nfctagemulator.ui.screen.CreateTagScreen
import com.example.nfctagemulator.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var showScanScreen = mutableStateOf(false)
    private var showCreateScreen = mutableStateOf(false)
    private var scannedTag = mutableStateOf<TagData?>(null)
    private var isEmulating = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reader = NfcReader(this)
        repository = TagRepository(this)
        emulator = TagEmulator(this)
        isEmulating.value = emulator.isEmulating()

        setContent {
            NfcTagEmulatorTheme {
                if (showScanScreen.value) {
                    ScanScreen(
                        repository = repository,
                        scannedTag = scannedTag.value,
                        onBackClick = {
                            showScanScreen.value = false
                            scannedTag.value = null
                        },
                        onCreateClick = {
                            showScanScreen.value = false
                            showCreateScreen.value = true
                        }
                    )
                } else if (showCreateScreen.value) {
                    CreateTagScreen(
                        repository = repository,
                        onBackClick = {
                            showCreateScreen.value = false
                        },
                        onTagCreated = {
                            showCreateScreen.value = false
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
        if (!isEmulating.value) {
            reader.enable(this)
        }
    }

    override fun onPause() {
        super.onPause()
        reader.disable(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isEmulating.value) {
            Toast.makeText(
                this,
                "Emulation mode is active. Stop emulation to scan.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val tagData = reader.readTag(intent)
        tagData?.let {
            // Check if tag with this UID already exists
            val existingTag = repository.getTagByUid(it.uid)

            if (existingTag != null) {
                // Tag already exists
                Toast.makeText(
                    this,
                    "⚠️ Tag already saved as \"${existingTag.name}\"",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Save new tag
                repository.saveTag(it)
                if (showScanScreen.value) {
                    scannedTag.value = it
                } else {
                    Toast.makeText(this, "✅ ${it.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateNfcState() {
        if (isEmulating.value) {
            reader.disable(this)
        } else {
            reader.enable(this)
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

    LaunchedEffect(Unit) {
        tags = repository.getAllTags()
        emulatingUid = emulator.getEmulatingTagUid()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NFC EMULATOR",
                            style = MaterialTheme.typography.headlineSmall,
                            color = NeonCyan
                        )

                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEmulating) NeonGreen else NeonCyan
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEmulating) "ACTIVE" else "READY",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isEmulating) NeonGreen else NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Scan and Create buttons
                    Row {
                        Button(
                            onClick = onScanClick,
                            enabled = !isEmulating,
                            modifier = Modifier
                                .height(40.dp)
                                .width(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(alpha = 0.2f),
                                contentColor = NeonCyan
                            )
                        ) {
                            Text("SCAN", fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (tags.isEmpty()) {
                EmptyState(
                    isEmulating = isEmulating,
                    onScanClick = onScanClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(tags) { tag ->
                        TagCard(
                            tag = tag,
                            isEmulating = tag.uid == emulatingUid,
                            onEmulateClick = { selectedTag ->
                                if (selectedTag.uid == emulatingUid) {
                                    emulator.setEmulatingTag(null)
                                    emulatingUid = null
                                    onEmulationStateChanged(false)
                                    Toast.makeText(context, "Emulation stopped", Toast.LENGTH_SHORT).show()
                                } else {
                                    emulator.setEmulatingTag(selectedTag)
                                    emulatingUid = selectedTag.uid
                                    onEmulationStateChanged(true)
                                    Toast.makeText(context, "Emulating: ${selectedTag.name}", Toast.LENGTH_SHORT).show()
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

    // Dialogs
    if (showRenameDialog && selectedTagForRename != null) {
        RenameDialog(
            tag = selectedTagForRename!!,
            currentName = newName,
            onNameChange = { newName = it },
            onConfirm = {
                if (newName.isNotBlank()) {
                    selectedTagForRename?.uid?.let { uid ->
                        repository.updateTagName(uid, newName)
                        tags = repository.getAllTags()

                        if (uid == emulatingUid) {
                            val updatedTag = tags.find { it.uid == uid }
                            updatedTag?.let { emulator.setEmulatingTag(it) }
                        }

                        showRenameDialog = false
                        Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog && tagToDelete != null) {
        DeleteDialog(
            tag = tagToDelete!!,
            onConfirm = {
                tagToDelete?.uid?.let { uid ->
                    repository.deleteTag(uid)
                    tags = repository.getAllTags()

                    if (uid == emulatingUid) {
                        emulator.setEmulatingTag(null)
                        emulatingUid = null
                        onEmulationStateChanged(false)
                    }

                    showDeleteDialog = false
                    Toast.makeText(context, "Tag deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
fun EmptyState(
    isEmulating: Boolean,
    onScanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated circle
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isEmulating) NeonGreen else NeonCyan,
                                if (isEmulating) NeonGreen.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEmulating) "⚡" else "NFC",
                    fontSize = 48.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEmulating)
                            "⚡ EMULATING"
                        else
                            "NO TAGS",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isEmulating) NeonGreen else NeonCyan
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEmulating)
                            "Your device is acting as an NFC tag.\nBring another device close to read it."
                        else
                            "Scan a physical tag or create a new one to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isEmulating) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier
                            .width(120.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan
                        )
                    ) {
                        Text("SCAN", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    tag: TagData,
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = NeonCyan,
        textContentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✏️ Rename Tag")
            }
        },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Tag name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                    focusedLabelColor = NeonCyan,
                    cursorColor = NeonCyan,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = NeonCyan
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun DeleteDialog(
    tag: TagData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = Color.Red,
        textContentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🗑️ Delete Tag")
            }
        },
        text = {
            Text(
                "Are you sure you want to delete \"${tag.name}\"?",
                color = Color.White.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text("DELETE")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("CANCEL")
            }
        }
    )
}