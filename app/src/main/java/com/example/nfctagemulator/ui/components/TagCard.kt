package com.example.nfctagemulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TagCard(
    tag: TagData,
    isEmulating: Boolean,
    onEmulateClick: (TagData) -> Unit,
    onRenameClick: (TagData) -> Unit,
    onDeleteClick: (TagData) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmulating)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEmulating) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Верхняя строка с именем и статусом
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Индикатор эмуляции
                    if (isEmulating) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isEmulating)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                        .format(Date(tag.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // UID
            Text(
                text = tag.uid ?: "UID неизвестен",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 14.sp
            )

            // Статус эмуляции подробно
            if (isEmulating) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚡ Сейчас эмулируется - телефон работает как эта метка",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка эмуляции (основная)
                Button(
                    onClick = { onEmulateClick(tag) },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmulating)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isEmulating) "⏹ Стоп"
                        else "▶ Старт",
                        fontSize = 14.sp
                    )
                }

                // Кнопка переименования
                OutlinedButton(
                    onClick = { onRenameClick(tag) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✎", fontSize = 18.sp)
                }

                // Кнопка удаления
                OutlinedButton(
                    onClick = { onDeleteClick(tag) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("×", fontSize = 20.sp)
                }
            }
        }
    }
}