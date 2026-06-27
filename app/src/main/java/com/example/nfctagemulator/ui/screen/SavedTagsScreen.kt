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
import androidx.compose.ui.platform.LocalConfiguration
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
    isNfcEnabled: Boolean,
    onEmulationStateChanged: (Boolean) -> Unit,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var tags by remember { mutableStateOf(repository.getAllTags()) }
    var emulatingUid by remember { mutableStateOf(emulator.getEmulatingTagUid()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTagForRename by remember { mutableStateOf<TagData?>(null) }
    var newName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<TagData?>(null) }

    LaunchedEffect(refreshTrigger, isNfcEnabled) {
        tags = repository.getAllTags()
        emulatingUid = emulator.getEmulatingTagUid()
    }

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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
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
                        .padding(
                            start = dimens.paddingSmall.dp,
                            top = dimens.paddingLarge.dp,
                            end = dimens.paddingSmall.dp,
                            bottom = dimens.paddingSmall.dp
                        )
                        .shadow(
                            elevation = if (emulating) 12.dp else 4.dp,
                            shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (emulating) SurfaceGlow else SurfaceDark
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.paddingMedium.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SAVED TAGS",
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonCyan,
                                fontSize = if (isLandscape) dimens.headerFontSize.sp else (dimens.headerFontSize + 4).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${tags.size} tag${if (tags.size != 1) "s" else ""} available",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = (dimens.bodyFontSize - 2).sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (emulating) NeonGreen.copy(alpha = 0.15f) else
                                if (!isNfcEnabled) Color.Red.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = dimens.paddingSmall.dp, vertical = dimens.paddingSmall.dp / 2)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                emulating -> NeonGreen
                                                !isNfcEnabled -> Color.Red
                                                else -> NeonCyan
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        emulating -> "EMULATING"
                                        !isNfcEnabled -> "NFC OFF"
                                        else -> "READY"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        emulating -> NeonGreen
                                        !isNfcEnabled -> Color.Red
                                        else -> NeonCyan
                                    },
                                    fontSize = if (isLandscape) (dimens.bodyFontSize - 3).sp else (dimens.bodyFontSize - 2).sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // NFC Warning Banner
            if (!isNfcEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.paddingMedium.dp, vertical = dimens.paddingSmall.dp),
                    shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.paddingMedium.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NFC is disabled. Please enable NFC to use emulation and scanning.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = dimens.bodyFontSize.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                            modifier = Modifier.size(if (isLandscape) 80.dp else 100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🏷️", fontSize = if (isLandscape) 40.sp else 48.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(dimens.paddingMedium.dp))
                        Text(
                            text = "No saved tags",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = dimens.titleFontSize.sp
                        )
                        Spacer(modifier = Modifier.height(dimens.paddingSmall.dp))
                        Text(
                            text = if (isLandscape) "Scan an NFC tag\nor create one" else "Scan a physical NFC tag\nor create a new one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            lineHeight = if (isLandscape) 18.sp else 22.sp,
                            fontSize = dimens.bodyFontSize.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = dimens.paddingMedium.dp,
                        end = dimens.paddingMedium.dp,
                        top = dimens.paddingSmall.dp,
                        bottom = dimens.paddingLarge.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall.dp)
                ) {
                    items(
                        items = tags,
                        key = { it.uid ?: it.name }
                    ) { tag ->
                        TagCard(
                            tag = tag,
                            isEmulating = tag.uid == emulatingUid,
                            isNfcEnabled = isNfcEnabled,
                            onEmulateClick = { selectedTag ->
                                if (!isNfcEnabled) {
                                    Toast.makeText(context, "Cannot emulate: NFC is disabled", Toast.LENGTH_SHORT).show()
                                    return@TagCard
                                }
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
            shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
            title = { Text("Rename Tag", fontWeight = FontWeight.Bold) },
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
                    shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = dimens.bodyFontSize.sp)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                ) {
                    Text("SAVE", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
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
            shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
            title = { Text("Delete Tag", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text(
                    "Are you sure you want to delete \"${tagToDelete?.name}\"?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = dimens.bodyFontSize.sp
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}
