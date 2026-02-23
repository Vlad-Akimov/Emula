package com.example.nfctagemulator.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.ui.components.TagCard
import com.example.nfctagemulator.ui.theme.NeonCyan
import com.example.nfctagemulator.ui.theme.SurfaceDark
import com.example.nfctagemulator.ui.theme.SurfaceGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTagsScreen(
    repository: TagRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val emulator = remember { TagEmulator(context) }

    var tags by remember { mutableStateOf(repository.getAllTags()) }
    var emulatingUid by remember { mutableStateOf(emulator.getEmulatingTagUid()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTagForRename by remember { mutableStateOf<TagData?>(null) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<TagData?>(null) }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SAVED TAGS",
                        color = NeonCyan
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text(text = "←", fontSize = 20.sp, color = NeonCyan)
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
        ) {
            if (tags.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No tags",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan a physical NFC tag or create a new one\nto start emulating",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        TagCard(
                            tag = tag,
                            isEmulating = tag.uid == emulatingUid,
                            onEmulateClick = { selectedTag ->
                                if (selectedTag.uid == emulatingUid) {
                                    emulator.setEmulatingTag(null)
                                    emulatingUid = null
                                    Toast.makeText(context, "Emulation stopped", Toast.LENGTH_SHORT).show()
                                } else {
                                    emulator.setEmulatingTag(selectedTag)
                                    emulatingUid = selectedTag.uid
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

    // Rename dialog
    if (showRenameDialog && selectedTagForRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SurfaceGlow,
            titleContentColor = NeonCyan,
            textContentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Rename Tag") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
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
                    onClick = {
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = NeonCyan
                    )
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Delete dialog
    if (showDeleteDialog && tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceGlow,
            titleContentColor = Color.Red,
            textContentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Tag") },
            text = {
                Text(
                    "Are you sure you want to delete \"${tagToDelete?.name}\"?",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
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

                            showDeleteDialog = false
                            Toast.makeText(context, "Tag deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}