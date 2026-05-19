package com.example.nfctagemulator.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.ui.components.GlowCard
import com.example.nfctagemulator.ui.theme.*
import java.nio.charset.Charset
import java.util.UUID

private const val TAG = "CreateTagScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTagScreen(
    repository: TagRepository,
    onBackClick: () -> Unit,
    onTagCreated: () -> Unit
) {
    val context = LocalContext.current
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var tagName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CreateTagType.URL) }
    var isCreating by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(dimens.paddingLarge.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "CREATE TAG",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                color = NeonCyan,
                fontSize = dimens.headerFontSize.sp,
                modifier = Modifier.padding(bottom = dimens.paddingMedium.dp)
            )

            // Type selection
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall.dp)
            ) {
                val types = listOf(
                    CreateTagType.URL to "🔗",
                    CreateTagType.TEXT to "📝",
                    CreateTagType.PHONE to "📞",
                    CreateTagType.EMAIL to "✉️",
                    CreateTagType.CONTACT to "👤"
                )
                items(types) { (type, emoji) ->
                    AdaptiveTypeCard(
                        emoji = emoji,
                        title = type.name,
                        isSelected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium.dp))

            // Input Form
            GlowCard(
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(
                    when (selectedType) {
                        CreateTagType.CONTACT -> NeonGreen.copy(alpha = 0.08f)
                        else -> NeonCyan.copy(alpha = 0.08f)
                    },
                    SurfaceDark
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.paddingMedium.dp)
                ) {
                    // Section header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = dimens.paddingMedium.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TAG DETAILS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                            fontSize = dimens.bodyFontSize.sp
                        )
                    }

                    // Tag Name Input
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text("Tag Name", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("My NFC Tag", fontFamily = FontFamily.Monospace) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(dimens.iconSize.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                            unfocusedBorderColor = (if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan).copy(alpha = 0.3f),
                            focusedLabelColor = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                            cursorColor = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = !isLandscape,
                        textStyle = LocalTextStyle.current.copy(fontSize = dimens.bodyFontSize.sp)
                    )

                    Spacer(modifier = Modifier.height(dimens.paddingMedium.dp))

                    // Dynamic content
                    when (selectedType) {
                        CreateTagType.CONTACT -> AdaptiveContactForm(
                            contactName = contactName,
                            onContactNameChange = { contactName = it },
                            contactPhone = contactPhone,
                            onContactPhoneChange = { contactPhone = it },
                            contactEmail = contactEmail,
                            onContactEmailChange = { contactEmail = it }
                        )
                        else -> AdaptiveContentInput(
                            value = content,
                            onValueChange = { content = it },
                            type = selectedType,
                            isMultiline = selectedType == CreateTagType.TEXT && !isLandscape
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.paddingLarge.dp))

                    // Create Button
                    val isFormValid = if (selectedType == CreateTagType.CONTACT) {
                        contactName.isNotBlank() || contactPhone.isNotBlank() || contactEmail.isNotBlank()
                    } else {
                        content.isNotBlank()
                    }

                    Button(
                        onClick = {
                            if (selectedType == CreateTagType.CONTACT) {
                                createContactTag(
                                    repository = repository,
                                    name = if (tagName.isNotBlank()) tagName else "Contact: ${contactName.take(20)}",
                                    contactName = contactName,
                                    contactPhone = contactPhone,
                                    contactEmail = contactEmail,
                                    onComplete = { success ->
                                        isCreating = false
                                        if (success) {
                                            Toast.makeText(context, "✅ Contact tag created", Toast.LENGTH_SHORT).show()
                                            onTagCreated()
                                        } else {
                                            Toast.makeText(context, "❌ Failed to create tag", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                createArtificialTag(
                                    repository = repository,
                                    name = if (tagName.isNotBlank()) tagName else getDefaultName(selectedType, content),
                                    content = content,
                                    type = selectedType,
                                    onComplete = { success ->
                                        isCreating = false
                                        if (success) {
                                            Toast.makeText(context, "✅ Tag created", Toast.LENGTH_SHORT).show()
                                            onTagCreated()
                                        } else {
                                            Toast.makeText(context, "❌ Failed to create tag", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isCreating && isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.buttonHeight.dp)
                            .shadow(
                                elevation = if (isFormValid && !isCreating) 8.dp else 0.dp,
                                shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                                clip = false
                            ),
                        shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = SurfaceGlow
                        )
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(dimens.iconSize.dp),
                                    tint = Color.Black
                                )
                                Text(
                                    text = if (isLandscape) "CREATE" else "CREATE NFC TAG",
                                    fontSize = if (isLandscape) dimens.bodyFontSize.sp else (dimens.bodyFontSize + 2).sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveTypeCard(
    emoji: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val cardWidth = if (isLandscape) 72.dp else 88.dp
    val cardHeight = if (isLandscape) 80.dp else 96.dp

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceGlow else SurfaceDark
        ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                (if (title == "CONTACT") NeonGreen else NeonCyan).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    } else Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.paddingSmall.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = if (isLandscape) 20.sp else 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLandscape && title.length > 4) title.take(3) else title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isLandscape) 9.sp else 11.sp
                    ),
                    color = if (isSelected)
                        (if (title == "CONTACT") NeonGreen else NeonCyan)
                    else
                        Color.White.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AdaptiveContentInput(
    value: String,
    onValueChange: (String) -> Unit,
    type: CreateTagType,
    isMultiline: Boolean = false
) {
    val dimens = getAdaptiveDimens()
    val icon = when (type) {
        CreateTagType.URL -> Icons.Default.Link
        CreateTagType.TEXT -> Icons.Default.Description
        CreateTagType.PHONE -> Icons.Default.Phone
        CreateTagType.EMAIL -> Icons.Default.Email
        else -> Icons.Default.Edit
    }

    val hint = when (type) {
        CreateTagType.URL -> "https://example.com"
        CreateTagType.TEXT -> "Enter your text here..."
        CreateTagType.PHONE -> "+1 234 567 890"
        CreateTagType.EMAIL -> "user@example.com"
        else -> "Enter content"
    }

    val label = when (type) {
        CreateTagType.URL -> "URL"
        CreateTagType.TEXT -> "Text Content"
        CreateTagType.PHONE -> "Phone Number"
        CreateTagType.EMAIL -> "Email Address"
        else -> "Content"
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = FontFamily.Monospace) },
        placeholder = { Text(hint, fontFamily = FontFamily.Monospace) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonPurple.copy(alpha = 0.7f),
                modifier = Modifier.size(dimens.iconSize.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurple,
            unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
            focusedLabelColor = NeonPurple,
            cursorColor = NeonPurple,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        singleLine = !isMultiline,
        minLines = if (isMultiline) 3 else 1,
        maxLines = if (isMultiline) 5 else 1,
        textStyle = LocalTextStyle.current.copy(fontSize = dimens.bodyFontSize.sp)
    )
}

@Composable
fun AdaptiveContactForm(
    contactName: String,
    onContactNameChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    contactEmail: String,
    onContactEmailChange: (String) -> Unit
) {
    val dimens = getAdaptiveDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ContactField(
                    value = contactName,
                    onValueChange = onContactNameChange,
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    iconColor = NeonGreen
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ContactField(
                    value = contactPhone,
                    onValueChange = onContactPhoneChange,
                    label = "Phone Number",
                    icon = Icons.Default.Phone,
                    iconColor = NeonGreen
                )
            }
        }
        Spacer(modifier = Modifier.height(dimens.paddingSmall.dp))
        ContactField(
            value = contactEmail,
            onValueChange = onContactEmailChange,
            label = "Email Address",
            icon = Icons.Default.Email,
            iconColor = NeonGreen
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium.dp)) {
            ContactField(
                value = contactName,
                onValueChange = onContactNameChange,
                label = "Full Name",
                icon = Icons.Default.Person,
                iconColor = NeonGreen
            )
            ContactField(
                value = contactPhone,
                onValueChange = onContactPhoneChange,
                label = "Phone Number",
                icon = Icons.Default.Phone,
                iconColor = NeonGreen
            )
            ContactField(
                value = contactEmail,
                onValueChange = onContactEmailChange,
                label = "Email Address",
                icon = Icons.Default.Email,
                iconColor = NeonGreen
            )
        }
    }
}

@Composable
fun ContactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    val dimens = getAdaptiveDimens()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = FontFamily.Monospace) },
        placeholder = {
            Text(
                when (label) {
                    "Full Name" -> "John Doe"
                    "Phone Number" -> "+1 234 567 890"
                    else -> "john@example.com"
                },
                fontFamily = FontFamily.Monospace
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor.copy(alpha = 0.7f),
                modifier = Modifier.size(dimens.iconSize.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardCornerRadius.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            unfocusedBorderColor = iconColor.copy(alpha = 0.3f),
            focusedLabelColor = iconColor,
            cursorColor = iconColor,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = dimens.bodyFontSize.sp)
    )
}

enum class CreateTagType {
    URL, TEXT, PHONE, EMAIL, CONTACT
}

fun getDefaultName(type: CreateTagType, content: String): String {
    return when (type) {
        CreateTagType.URL -> "URL: ${content.take(20)}"
        CreateTagType.TEXT -> "Text: ${content.take(20)}"
        CreateTagType.PHONE -> "Phone: $content"
        CreateTagType.EMAIL -> "Email: $content"
        CreateTagType.CONTACT -> "Contact"
    }
}

fun createArtificialTag(
    repository: TagRepository,
    name: String,
    content: String,
    type: CreateTagType,
    onComplete: (Boolean) -> Unit
) {
    try {
        val uid = generateArtificialUid()
        val ndefMessage = createNdefMessage(content, type)
        val tagType = convertToTagType(type)

        val tag = TagData(
            uid = uid,
            name = name,
            type = tagType,
            ndefMessage = ndefMessage,
            techList = listOf("android.nfc.tech.Ndef", "android.nfc.tech.NdefFormatable"),
            rawData = uid.toByteArray(Charset.forName("UTF-8"))
        )

        repository.saveTag(tag)
        onComplete(true)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating tag", e)
        onComplete(false)
    }
}

fun createContactTag(
    repository: TagRepository,
    name: String,
    contactName: String,
    contactPhone: String,
    contactEmail: String,
    onComplete: (Boolean) -> Unit
) {
    try {
        val uid = generateArtificialUid()
        val ndefMessage = createVCardNdefMessage(contactName, contactPhone, contactEmail)

        val tag = TagData(
            uid = uid,
            name = name,
            type = TagType.NDEF_VCARD,
            ndefMessage = ndefMessage,
            techList = listOf("android.nfc.tech.Ndef", "android.nfc.tech.NdefFormatable"),
            rawData = uid.toByteArray(Charset.forName("UTF-8")),
            contactName = contactName.ifBlank { null },
            contactPhone = contactPhone.ifBlank { null },
            contactEmail = contactEmail.ifBlank { null }
        )

        repository.saveTag(tag)
        onComplete(true)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating contact tag", e)
        onComplete(false)
    }
}

fun generateArtificialUid(): String {
    val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 14)
    return "AR$uuid".uppercase()
}

fun createNdefMessage(content: String, type: CreateTagType): ByteArray {
    return when (type) {
        CreateTagType.URL -> createUrlNdefMessage(content)
        CreateTagType.TEXT -> createTextNdefMessage(content)
        CreateTagType.PHONE -> createPhoneNdefMessage(content)
        CreateTagType.EMAIL -> createEmailNdefMessage(content)
        CreateTagType.CONTACT -> throw IllegalArgumentException("Use createVCardNdefMessage for contacts")
    }
}

fun createVCardNdefMessage(
    contactName: String,
    contactPhone: String,
    contactEmail: String
): ByteArray {
    val vcard = buildString {
        append("BEGIN:VCARD\n")
        append("VERSION:3.0\n")
        if (contactName.isNotBlank()) append("FN:$contactName\n")
        if (contactPhone.isNotBlank()) append("TEL:$contactPhone\n")
        if (contactEmail.isNotBlank()) append("EMAIL:$contactEmail\n")
        append("END:VCARD")
    }

    val mimeRecord = createNdefMimeRecord("text/vcard", vcard)
    val message = ByteArray(2 + mimeRecord.size)
    message[0] = ((mimeRecord.size shr 8) and 0xFF).toByte()
    message[1] = (mimeRecord.size and 0xFF).toByte()
    System.arraycopy(mimeRecord, 0, message, 2, mimeRecord.size)

    return message
}

fun createUrlNdefMessage(url: String): ByteArray {
    val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }

    val uriRecord = createNdefUriRecord(fullUrl)
    val message = ByteArray(2 + uriRecord.size)
    message[0] = ((uriRecord.size shr 8) and 0xFF).toByte()
    message[1] = (uriRecord.size and 0xFF).toByte()
    System.arraycopy(uriRecord, 0, message, 2, uriRecord.size)
    return message
}

fun createTextNdefMessage(text: String): ByteArray {
    val textRecord = createNdefTextRecord(text)
    val message = ByteArray(2 + textRecord.size)
    message[0] = ((textRecord.size shr 8) and 0xFF).toByte()
    message[1] = (textRecord.size and 0xFF).toByte()
    System.arraycopy(textRecord, 0, message, 2, textRecord.size)
    return message
}

fun createPhoneNdefMessage(phone: String): ByteArray {
    val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
    val telUri = "tel:$cleanPhone"

    val header: Byte = 0xD1.toByte()
    val typeLength: Byte = 0x01
    val type: Byte = 0x55.toByte()
    val uriCode: Byte = 0x05
    val cleanUri = telUri.substring(4)
    val uriBytes = cleanUri.toByteArray(Charset.forName("UTF-8"))

    val payloadLength = (uriBytes.size + 1).toByte()

    val record = ByteArray(4 + 1 + uriBytes.size)
    var pos = 0
    record[pos++] = header
    record[pos++] = typeLength
    record[pos++] = payloadLength
    record[pos++] = type
    record[pos++] = uriCode
    uriBytes.forEach { record[pos++] = it }

    val message = ByteArray(2 + record.size)
    message[0] = ((record.size shr 8) and 0xFF).toByte()
    message[1] = (record.size and 0xFF).toByte()
    System.arraycopy(record, 0, message, 2, record.size)

    return message
}

fun createEmailNdefMessage(email: String): ByteArray {
    val cleanEmail = email.trim()
    val mailtoUri = if (!cleanEmail.startsWith("mailto:")) {
        "mailto:$cleanEmail"
    } else {
        cleanEmail
    }

    val uriRecord = createNdefUriRecord(mailtoUri)
    val message = ByteArray(2 + uriRecord.size)
    message[0] = ((uriRecord.size shr 8) and 0xFF).toByte()
    message[1] = (uriRecord.size and 0xFF).toByte()
    System.arraycopy(uriRecord, 0, message, 2, uriRecord.size)
    return message
}

fun createNdefUriRecord(uri: String): ByteArray {
    val header: Byte = 0xD1.toByte()
    val typeLength: Byte = 0x01
    val (uriCode, cleanUri) = getUriCodeAndCleanUrl(uri)
    val uriBytes = cleanUri.toByteArray(Charset.forName("UTF-8"))
    val payloadLength = (uriBytes.size + 1).toByte()
    val type: Byte = 0x55.toByte()

    val record = ByteArray(4 + 1 + uriBytes.size)
    var pos = 0
    record[pos++] = header
    record[pos++] = typeLength
    record[pos++] = payloadLength
    record[pos++] = type
    record[pos++] = uriCode
    uriBytes.forEach { record[pos++] = it }

    return record
}

fun createNdefTextRecord(text: String): ByteArray {
    val header: Byte = 0xD1.toByte()
    val typeLength: Byte = 0x01
    val languageCode = "en".toByteArray(Charset.forName("US-ASCII"))
    val textBytes = text.toByteArray(Charset.forName("UTF-8"))
    val statusByte = (languageCode.size and 0x3F).toByte()
    val payloadLength = (1 + languageCode.size + textBytes.size).toByte()
    val type: Byte = 0x54.toByte()

    val record = ByteArray(4 + 1 + languageCode.size + textBytes.size)
    var pos = 0
    record[pos++] = header
    record[pos++] = typeLength
    record[pos++] = payloadLength
    record[pos++] = type
    record[pos++] = statusByte
    languageCode.forEach { record[pos++] = it }
    textBytes.forEach { record[pos++] = it }

    return record
}

fun createNdefMimeRecord(mimeType: String, content: String): ByteArray {
    val header: Byte = 0xD2.toByte()
    val typeBytes = mimeType.toByteArray(Charset.forName("US-ASCII"))
    val contentBytes = content.toByteArray(Charset.forName("UTF-8"))
    val typeLength = typeBytes.size.toByte()
    val payloadLength = contentBytes.size.toByte()

    val record = ByteArray(3 + typeBytes.size + contentBytes.size)
    var pos = 0
    record[pos++] = header
    record[pos++] = typeLength
    record[pos++] = payloadLength
    typeBytes.forEach { record[pos++] = it }
    contentBytes.forEach { record[pos++] = it }

    return record
}

fun getUriCodeAndCleanUrl(url: String): Pair<Byte, String> {
    return when {
        url.startsWith("http://www.") -> Pair(0x01, url.substring(11))
        url.startsWith("https://www.") -> Pair(0x02, url.substring(12))
        url.startsWith("http://") -> Pair(0x03, url.substring(7))
        url.startsWith("https://") -> Pair(0x04, url.substring(8))
        url.startsWith("tel:") -> Pair(0x05, url.substring(4))
        url.startsWith("mailto:") -> Pair(0x06, url.substring(7))
        url.startsWith("ftp://") -> Pair(0x07, url.substring(6))
        url.startsWith("ftps://") -> Pair(0x08, url.substring(7))
        url.startsWith("geo:") -> Pair(0x09, url.substring(4))
        url.startsWith("sms:") -> Pair(0x10, url.substring(4))
        else -> Pair(0x00, url)
    }
}

fun convertToTagType(type: CreateTagType): TagType {
    return when (type) {
        CreateTagType.URL -> TagType.NDEF_URI
        CreateTagType.TEXT -> TagType.NDEF_TEXT
        CreateTagType.PHONE -> TagType.NDEF_URI
        CreateTagType.EMAIL -> TagType.NDEF_URI
        CreateTagType.CONTACT -> TagType.NDEF_VCARD
    }
}