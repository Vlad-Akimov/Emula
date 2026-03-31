package com.example.nfctagemulator.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var tagName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CreateTagType.URL) }
    var isCreating by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "create")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

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
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CREATE TAG",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = NeonCyan,
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(8.dp),
                        clip = false
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Type selection row with modern cards
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val types = listOf(
                    CreateTagType.URL to "🔗",
                    CreateTagType.TEXT to "📝",
                    CreateTagType.PHONE to "📞",
                    CreateTagType.EMAIL to "✉️",
                    CreateTagType.CONTACT to "👤"
                )
                items(types) { (type, emoji) ->
                    ModernTypeCard(
                        emoji = emoji,
                        title = type.name,
                        isSelected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Input Form with GlowCard
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
                        .padding(24.dp)
                ) {
                    // Section header with animated dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp)
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
                            color = if (selectedType == CreateTagType.CONTACT) NeonGreen else NeonCyan
                        )
                    }

                    // Tag Name Input
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = {
                            Text(
                                "Tag Name",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                "My NFC Tag",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic content based on selected type
                    when (selectedType) {
                        CreateTagType.CONTACT -> ContactForm(
                            contactName = contactName,
                            onContactNameChange = { contactName = it },
                            contactPhone = contactPhone,
                            onContactPhoneChange = { contactPhone = it },
                            contactEmail = contactEmail,
                            onContactEmailChange = { contactEmail = it }
                        )
                        else -> ContentInput(
                            value = content,
                            onValueChange = { content = it },
                            type = selectedType,
                            isMultiline = selectedType == CreateTagType.TEXT
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Create Button with neon effect
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
                                            Toast.makeText(
                                                context,
                                                "✅ Contact tag created",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onTagCreated()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "❌ Failed to create tag",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                            Toast.makeText(
                                                context,
                                                "✅ Tag created",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onTagCreated()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "❌ Failed to create tag",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isCreating && isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = if (isFormValid && !isCreating) 8.dp else 0.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            ),
                        shape = RoundedCornerShape(12.dp),
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
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Black
                                )
                                Text(
                                    text = "CREATE NFC TAG",
                                    fontSize = 14.sp,
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
fun ModernTypeCard(
    emoji: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "type_card")
    val cardScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .width(88.dp)
            .height(96.dp)
            .then(if (isSelected) Modifier.scale(cardScale) else Modifier),
        shape = RoundedCornerShape(16.dp),
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
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
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
fun ContentInput(
    value: String,
    onValueChange: (String) -> Unit,
    type: CreateTagType,
    isMultiline: Boolean = false
) {
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
        label = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
        },
        placeholder = {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonPurple.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
        maxLines = if (isMultiline) 5 else 1
    )
}

@Composable
fun ContactForm(
    contactName: String,
    onContactNameChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    contactEmail: String,
    onContactEmailChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = contactName,
            onValueChange = onContactNameChange,
            label = {
                Text(
                    "Full Name",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            placeholder = {
                Text(
                    "John Doe",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = NeonGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = NeonGreen.copy(alpha = 0.3f),
                focusedLabelColor = NeonGreen,
                cursorColor = NeonGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = contactPhone,
            onValueChange = onContactPhoneChange,
            label = {
                Text(
                    "Phone Number",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            placeholder = {
                Text(
                    "+1 234 567 890",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = NeonGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = NeonGreen.copy(alpha = 0.3f),
                focusedLabelColor = NeonGreen,
                cursorColor = NeonGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = contactEmail,
            onValueChange = onContactEmailChange,
            label = {
                Text(
                    "Email Address",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            placeholder = {
                Text(
                    "john@example.com",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = NeonGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = NeonGreen.copy(alpha = 0.3f),
                focusedLabelColor = NeonGreen,
                cursorColor = NeonGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )
    }
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
        Log.d(TAG, "Creating artificial tag: name=$name, type=$type, content=$content")

        val uid = generateArtificialUid()
        Log.d(TAG, "Generated UID: $uid")

        val ndefMessage = createNdefMessage(content, type)
        Log.d(TAG, "Created NDEF message size: ${ndefMessage.size} bytes")

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
        Log.d(TAG, "Creating contact tag: name=$name")

        val uid = generateArtificialUid()
        Log.d(TAG, "Generated UID: $uid")

        val ndefMessage = createVCardNdefMessage(contactName, contactPhone, contactEmail)
        Log.d(TAG, "Created vCard NDEF message size: ${ndefMessage.size} bytes")

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
    // Build vCard string
    val vcard = buildString {
        append("BEGIN:VCARD\n")
        append("VERSION:3.0\n")
        if (contactName.isNotBlank()) append("FN:$contactName\n")
        if (contactPhone.isNotBlank()) append("TEL:$contactPhone\n")
        if (contactEmail.isNotBlank()) append("EMAIL:$contactEmail\n")
        append("END:VCARD")
    }

    // Create MIME record for vCard
    val mimeRecord = createNdefMimeRecord("text/vcard", vcard)

    // Wrap with NLEN (2 bytes length)
    val message = ByteArray(2 + mimeRecord.size)
    message[0] = ((mimeRecord.size shr 8) and 0xFF).toByte()
    message[1] = (mimeRecord.size and 0xFF).toByte()
    System.arraycopy(mimeRecord, 0, message, 2, mimeRecord.size)

    return message
}

fun createUrlNdefMessage(url: String): ByteArray {
    // Ensure URL has protocol
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
    // Clean phone number from non-digit characters except +
    val cleanPhone = phone.replace(Regex("[^\\d+]"), "")

    // Create proper tel: URI
    val telUri = "tel:$cleanPhone"
    Log.d(TAG, "Creating phone NDEF with URI: $telUri")

    // Create a proper NDEF record for tel URI
    // Using TNF_WELL_KNOWN with type "U" for URI
    val header: Byte = 0xD1.toByte() // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01 (NFC Forum well-known type)
    val typeLength: Byte = 0x01
    val type: Byte = 0x55.toByte() // 'U'

    // For tel: scheme, we use URI code 0x05
    val uriCode: Byte = 0x05
    val cleanUri = telUri.substring(4) // Remove "tel:" prefix
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

    // Wrap with NLEN (2 bytes length)
    val message = ByteArray(2 + record.size)
    message[0] = ((record.size shr 8) and 0xFF).toByte()
    message[1] = (record.size and 0xFF).toByte()
    System.arraycopy(record, 0, message, 2, record.size)

    return message
}

fun createEmailNdefMessage(email: String): ByteArray {
    // Validate email format (basic check)
    val cleanEmail = email.trim()

    // Create proper mailto: URI
    val mailtoUri = if (!cleanEmail.startsWith("mailto:")) {
        "mailto:$cleanEmail"
    } else {
        cleanEmail
    }
    Log.d(TAG, "Creating email NDEF with URI: $mailtoUri")

    // Use URI record for email (this will work with mailto: scheme)
    val uriRecord = createNdefUriRecord(mailtoUri)
    val message = ByteArray(2 + uriRecord.size)
    message[0] = ((uriRecord.size shr 8) and 0xFF).toByte()
    message[1] = (uriRecord.size and 0xFF).toByte()
    System.arraycopy(uriRecord, 0, message, 2, uriRecord.size)
    return message
}

fun createNdefUriRecord(uri: String): ByteArray {
    val header: Byte = 0xD1.toByte() // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01 (NFC Forum well-known type)
    val typeLength: Byte = 0x01
    val (uriCode, cleanUri) = getUriCodeAndCleanUrl(uri)
    val uriBytes = cleanUri.toByteArray(Charset.forName("UTF-8"))
    val payloadLength = (uriBytes.size + 1).toByte()
    val type: Byte = 0x55.toByte() // 'U'

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
    val header: Byte = 0xD1.toByte() // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01
    val typeLength: Byte = 0x01
    val languageCode = "en".toByteArray(Charset.forName("US-ASCII"))
    val textBytes = text.toByteArray(Charset.forName("UTF-8"))
    val statusByte = (languageCode.size and 0x3F).toByte() // Bit 7 = 0 (UTF-8)
    val payloadLength = (1 + languageCode.size + textBytes.size).toByte()
    val type: Byte = 0x54.toByte() // 'T'

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
    // Header: MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x02 (MIME media type)
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