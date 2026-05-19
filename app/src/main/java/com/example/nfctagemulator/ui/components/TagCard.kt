package com.example.nfctagemulator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalConfiguration
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
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

    val cornerRadius = dimens.cardCornerRadius.dp
    val innerCornerRadius = (dimens.cardCornerRadius - 2).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isEmulating) 16.dp else 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                if (isEmulating) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.minDimension / 1.5f
                        ),
                        radius = size.minDimension / 1.5f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
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
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(1.5.dp)
                .background(
                    color = if (isEmulating) SurfaceGlow else SurfaceDark,
                    shape = RoundedCornerShape(innerCornerRadius)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.paddingMedium.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isEmulating) NeonCyan else Color.White,
                            fontSize = dimens.titleFontSize.sp,
                            maxLines = if (isLandscape) 2 else 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = formatUid(tag.uid),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = (dimens.bodyFontSize - 2).sp
                    )
                    if (tag.type == TagType.NDEF_VCARD) {
                        Text(
                            text = buildString {
                                tag.contactName?.let { append(it) }
                                if (tag.contactPhone != null && tag.contactName != null) append(" • ")
                                tag.contactPhone?.let { append(it.take(12)) }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurple,
                            fontSize = (dimens.bodyFontSize - 4).sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onDeleteClick(tag) },
                        modifier = Modifier.size((dimens.iconSize + 8).dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.Red.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(dimens.iconSize.dp)
                        )
                    }

                    IconButton(
                        onClick = { onRenameClick(tag) },
                        modifier = Modifier.size((dimens.iconSize + 8).dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(dimens.iconSize.dp)
                        )
                    }

                    Button(
                        onClick = { onEmulateClick(tag) },
                        modifier = Modifier
                            .height((dimens.buttonHeight - 8).dp)
                            .widthIn(min = if (isLandscape) 70.dp else 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmulating)
                                NeonGreen.copy(alpha = 0.15f)
                            else
                                NeonCyan.copy(alpha = 0.15f),
                            contentColor = if (isEmulating) NeonGreen else NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isEmulating) "⏹️" else "▶️",
                                fontSize = (dimens.bodyFontSize - 2).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEmulating) "STOP" else if (isLandscape) "EMUL" else "EMULATE",
                                fontSize = (dimens.bodyFontSize - 4).sp,
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
