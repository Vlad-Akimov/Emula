package com.example.nfctagemulator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isEmulating) 16.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                if (isEmulating) {
                    val strokeWidth = 2.dp.toPx()
                    for (i in 0..1) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 1.5f
                            ),
                            radius = size.minDimension / 1.5f + (i * 4).dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isEmulating) SurfaceGlow else SurfaceDark
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isEmulating)
                            listOf(NeonCyan, NeonPurple, NeonPink, NeonCyan)
                        else
                            listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(1.5.dp)
                .background(
                    color = if (isEmulating) SurfaceGlow else SurfaceDark,
                    shape = RoundedCornerShape(22.5.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag icon and info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(modifier = Modifier.width(12.dp))

                    // Text info
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isEmulating) NeonCyan else Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Text(
                            text = formatUid(tag.uid),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
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
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Actions
                Row {
                    // Rename button
                    IconButton(
                        onClick = { onRenameClick(tag) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { onDeleteClick(tag) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.Red.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Emulate button
                    Button(
                        onClick = { onEmulateClick(tag) },
                        modifier = Modifier
                            .height(36.dp)
                            .widthIn(min = 88.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmulating)
                                NeonGreen.copy(alpha = 0.15f)
                            else
                                NeonCyan.copy(alpha = 0.15f),
                            contentColor = if (isEmulating) NeonGreen else NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isEmulating) "⏹️" else "▶️",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEmulating) "STOP" else "EMULATE",
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
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