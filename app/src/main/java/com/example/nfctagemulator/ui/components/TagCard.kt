package com.example.nfctagemulator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
import com.example.nfctagemulator.ui.theme.*

@Composable
fun TagCard(
    tag: TagData,
    isEmulating: Boolean,
    onEmulateClick: (TagData) -> Unit,
    onRenameClick: (TagData) -> Unit,
    onDeleteClick: (TagData) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tag_card")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isEmulating) 12.dp else 4.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmulating) SurfaceGlow else SurfaceDark
        )
    ) {
        // Добавляем градиентную границу вручную
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isEmulating)
                            listOf(NeonCyan, NeonPurple, NeonCyan)
                        else
                            listOf(NeonCyan.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(1.dp) // Толщина границы
                .background(
                    color = if (isEmulating) SurfaceGlow else SurfaceDark,
                    shape = RoundedCornerShape(23.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(modifier = Modifier.width(12.dp))

                    // Text info
                    Column {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isEmulating) NeonCyan else Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = formatUid(tag.uid),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        // Show contact preview if it's a contact
                        if (tag.type == TagType.NDEF_VCARD) {
                            Text(
                                text = buildString {
                                    tag.contactName?.let { append(it) }
                                    if (tag.contactPhone != null && tag.contactName != null) append(" • ")
                                    tag.contactPhone?.let { append(it) }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPurple,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Actions
                Row {
                    // Rename button
                    Button(
                        onClick = { onRenameClick(tag) },
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.1f),
                            contentColor = NeonCyan
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("✏️", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete button
                    Button(
                        onClick = { onDeleteClick(tag) },
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.1f),
                            contentColor = Color.Red
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🗑️", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Emulate button
                    Button(
                        onClick = { onEmulateClick(tag) },
                        modifier = Modifier
                            .height(36.dp)
                            .widthIn(min = 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmulating)
                                NeonGreen.copy(alpha = 0.2f)
                            else
                                NeonCyan.copy(alpha = 0.2f),
                            contentColor = if (isEmulating) NeonGreen else NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (isEmulating) "⏹️ STOP" else "▶️ EMULATE",
                            fontSize = 10.sp,
                            maxLines = 1
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