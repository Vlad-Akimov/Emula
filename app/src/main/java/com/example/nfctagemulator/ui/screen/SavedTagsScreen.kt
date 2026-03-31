package com.example.nfctagemulator.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.ui.components.TagCard
import com.example.nfctagemulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SavedTagsScreen(
    repository: TagRepository,
    emulator: TagEmulator,
    isEmulating: Boolean,
    onEmulationStateChanged: (Boolean) -> Unit,
    refreshTrigger: Int = 0 // Add refresh trigger parameter
) {
    val context = LocalContext.current

    var tags by remember { mutableStateOf(repository.getAllTags()) }
    var emulatingUid by remember { mutableStateOf(emulator.getEmulatingTagUid()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTagForRename by remember { mutableStateOf<TagData?>(null) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<TagData?>(null) }

    // Refresh tags when refreshTrigger changes or when entering the screen
    LaunchedEffect(refreshTrigger) {
        tags = repository.getAllTags()
        emulatingUid = emulator.getEmulatingTagUid()
    }

    // Also refresh when emulation state changes (in case name was updated)
    LaunchedEffect(isEmulating) {
        tags = repository.getAllTags()
        emulatingUid = emulator.getEmulatingTagUid()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark, Color(0xFF0A0A12)),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Animated header with status
            AnimatedContent(
                targetState = isEmulating,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(animationSpec = tween(300))
                },
                label = "header"
            ) { emulating ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .shadow(
                            elevation = if (emulating) 12.dp else 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (emulating) SurfaceGlow else SurfaceDark
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
                                text = "SAVED TAGS",
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonCyan,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${tags.size} tag${if (tags.size != 1) "s" else ""} available",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        // Status indicator with pulsing effect
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (emulating) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (emulating) NeonGreen else NeonCyan
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (emulating) "EMULATING" else "READY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (emulating) NeonGreen else NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Content
            if (tags.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceGlow,
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🏷️",
                                    fontSize = 48.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No saved tags",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan a physical NFC tag\nor create a new one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = tags,
                        key = { it.uid ?: it.name }
                    ) { tag ->
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

    // Rename dialog
    if (showRenameDialog && selectedTagForRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SurfaceGlow,
            titleContentColor = NeonCyan,
            textContentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Rename Tag",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tag name", color = NeonCyan) },
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
                    Text("SAVE", fontWeight = FontWeight.Medium)
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
            title = {
                Text(
                    "Delete Tag",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            },
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
                                onEmulationStateChanged(false)
                            }

                            showDeleteDialog = false
                            Toast.makeText(context, "Tag deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("DELETE", fontWeight = FontWeight.Medium)
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