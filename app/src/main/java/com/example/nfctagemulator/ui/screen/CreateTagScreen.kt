package com.example.nfctagemulator.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.ui.theme.NeonCyan
import com.example.nfctagemulator.ui.theme.NeonGreen
import com.example.nfctagemulator.ui.theme.NeonPurple
import com.example.nfctagemulator.ui.theme.SurfaceDark
import com.example.nfctagemulator.ui.theme.SurfaceGlow
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

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "CREATE NFC TAG",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type selection row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TypeCard(
                        emoji = "🔗",
                        title = "URL",
                        description = "Website link",
                        isSelected = selectedType == CreateTagType.URL,
                        onClick = { selectedType = CreateTagType.URL }
                    )
                }
                item {
                    TypeCard(
                        emoji = "📝",
                        title = "Text",
                        description = "Plain text",
                        isSelected = selectedType == CreateTagType.TEXT,
                        onClick = { selectedType = CreateTagType.TEXT }
                    )
                }
                item {
                    TypeCard(
                        emoji = "📞",
                        title = "Phone",
                        description = "Call number",
                        isSelected = selectedType == CreateTagType.PHONE,
                        onClick = { selectedType = CreateTagType.PHONE }
                    )
                }
                item {
                    TypeCard(
                        emoji = "✉️",
                        title = "Email",
                        description = "Send email",
                        isSelected = selectedType == CreateTagType.EMAIL,
                        onClick = { selectedType = CreateTagType.EMAIL }
                    )
                }
                item {
                    TypeCard(
                        emoji = "👤",
                        title = "Contact",
                        description = "Full contact",
                        isSelected = selectedType == CreateTagType.CONTACT,
                        onClick = { selectedType = CreateTagType.CONTACT }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "TAG DETAILS",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Tag Name Input
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text(text = "Tag Name (optional)") },
                        placeholder = { Text(text = "My NFC Tag") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                            focusedLabelColor = NeonCyan,
                            cursorColor = NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedType == CreateTagType.CONTACT) {
                        // Contact Form
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text(text = "Full Name") },
                            placeholder = { Text(text = "John Doe") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
                                focusedLabelColor = NeonPurple,
                                cursorColor = NeonPurple,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text(text = "Phone Number") },
                            placeholder = { Text(text = "+1234567890") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
                                focusedLabelColor = NeonPurple,
                                cursorColor = NeonPurple,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text(text = "Email Address") },
                            placeholder = { Text(text = "john@example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
                                focusedLabelColor = NeonPurple,
                                cursorColor = NeonPurple,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    } else {
                        // Single content input for other types
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text(text = getInputLabel(selectedType)) },
                            placeholder = { Text(text = getInputPlaceholder(selectedType)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
                                focusedLabelColor = NeonPurple,
                                cursorColor = NeonPurple,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = selectedType != CreateTagType.TEXT,
                            maxLines = if (selectedType == CreateTagType.TEXT) 3 else 1
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Button
                    Button(
                        onClick = {
                            if (selectedType == CreateTagType.CONTACT) {
                                if (contactName.isNotBlank() || contactPhone.isNotBlank() || contactEmail.isNotBlank()) {
                                    isCreating = true
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
                                                    "✅ Contact tag created successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onTagCreated()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "❌ Failed to create contact tag",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please enter at least one contact field",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                if (content.isNotBlank()) {
                                    isCreating = true
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
                                                    "✅ Tag created successfully",
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
                                    Toast.makeText(
                                        context,
                                        "Please enter ${getInputLabel(selectedType).lowercase()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !isCreating && (
                                (selectedType == CreateTagType.CONTACT &&
                                        (contactName.isNotBlank() || contactPhone.isNotBlank() || contactEmail.isNotBlank())) ||
                                        (selectedType != CreateTagType.CONTACT && content.isNotBlank())
                                ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = SurfaceGlow
                        )
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "CREATE NFC TAG",
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypeCard(
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(90.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceGlow else SurfaceDark
        ),
        onClick = onClick
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
                fontSize = 24.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) NeonPurple else Color.White.copy(alpha = 0.5f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

enum class CreateTagType {
    URL, TEXT, PHONE, EMAIL, CONTACT
}

fun getInputLabel(type: CreateTagType): String {
    return when (type) {
        CreateTagType.URL -> "URL"
        CreateTagType.TEXT -> "Text Content"
        CreateTagType.PHONE -> "Phone Number"
        CreateTagType.EMAIL -> "Email Address"
        CreateTagType.CONTACT -> "Contact Information"
    }
}

fun getInputPlaceholder(type: CreateTagType): String {
    return when (type) {
        CreateTagType.URL -> "https://example.com"
        CreateTagType.TEXT -> "Enter your text here..."
        CreateTagType.PHONE -> "+1234567890"
        CreateTagType.EMAIL -> "user@example.com"
        CreateTagType.CONTACT -> "Multiple fields"
    }
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

    // Use URI record for phone (this will work with tel: scheme)
    val uriRecord = createNdefUriRecord(telUri)
    val message = ByteArray(2 + uriRecord.size)
    message[0] = ((uriRecord.size shr 8) and 0xFF).toByte()
    message[1] = (uriRecord.size and 0xFF).toByte()
    System.arraycopy(uriRecord, 0, message, 2, uriRecord.size)
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