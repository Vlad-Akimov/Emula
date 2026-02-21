package com.example.nfctagemulator.nfc.emulator

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.nfctagemulator.data.repository.TagRepository
import java.nio.charset.Charset

class TagHostApduService : HostApduService() {

    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator

    companion object {
        private const val TAG = "TagHostApduService"

        // ISO 7816 status words
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69, 0x86.toByte())
        private val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        private val SW_EOF = byteArrayOf(0x62, 0x82.toByte())

        // Standard NDEF AID for Type 4 Tag (NFC Forum)
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        // File IDs for Type 4 Tag
        private const val FILE_ID_CC = 0xE103  // Capability Container
        private const val FILE_ID_NDEF = 0xE104 // NDEF File

        // Capability Container for Type 4 Tag (validated format)
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,                    // CCLEN = 15 bytes
            0x20,                           // Mapping Version 2.0
            0x00.toByte(), 0x40.toByte(),   // Maximum R-APDU data size = 64
            0x00.toByte(), 0x40.toByte(),   // Maximum C-APDU data size = 64
            0x04,                           // NDEF File Control TLV present
            0x06,                           // TLV length = 6
            (FILE_ID_NDEF shr 8).toByte(),   // NDEF File ID (high byte)
            (FILE_ID_NDEF and 0xFF).toByte(), // NDEF File ID (low byte)
            0x08, 0x00,                     // Maximum NDEF size = 2048 bytes
            0x00,                           // Read access always
            0x00                            // Write access always
        )

        // Commands
        private val INS_SELECT: Byte = 0xA4.toByte()
        private val INS_READ_BINARY: Byte = 0xB0.toByte()
        private val INS_UPDATE_BINARY: Byte = 0xD6.toByte()

        // SELECT P1/P2 values
        private const val P1_SELECT_BY_NAME = 0x04
        private const val P1_SELECT_BY_FILE_ID = 0x00
    }

    private var currentFile: Int = 0
    private var ndefData: ByteArray? = null
    private var currentUid: String? = null

    override fun onCreate() {
        super.onCreate()
        repository = TagRepository(this)
        emulator = TagEmulator(this)
        Log.d(TAG, "TagHostApduService created")
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        try {
            Log.d(TAG, "Received APDU: ${bytesToHex(commandApdu)}")

            // Check if we're emulating
            val emulatingUid = emulator.getEmulatingTagUid() ?: run {
                Log.d(TAG, "No tag emulating")
                return SW_FILE_NOT_FOUND
            }

            // Update if UID changed
            if (currentUid != emulatingUid) {
                currentUid = emulatingUid
                ndefData = null
                currentFile = 0
                Log.d(TAG, "Now emulating: $emulatingUid")
            }

            // Load NDEF data if needed
            if (ndefData == null) {
                ndefData = loadNdefData(emulatingUid)
                if (ndefData != null) {
                    Log.d(TAG, "NDEF data loaded, size: ${ndefData!!.size} bytes")
                    Log.d(TAG, "NDEF data hex: ${bytesToHex(ndefData!!)}")
                }
            }

            // Parse command
            if (commandApdu.size < 4) {
                return SW_WRONG_LENGTH
            }

            val ins = commandApdu[1]
            val p1 = commandApdu[2].toInt() and 0xFF
            val p2 = commandApdu[3].toInt() and 0xFF

            // Handle command based on INS
            return when (ins) {
                INS_SELECT -> handleSelect(p1, p2, commandApdu)
                INS_READ_BINARY -> handleReadBinary(p1, p2, commandApdu)
                INS_UPDATE_BINARY -> handleUpdateBinary(p1, p2, commandApdu)
                else -> {
                    Log.d(TAG, "Unknown INS: 0x${ins.toString(16)}")
                    SW_COMMAND_NOT_ALLOWED
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing APDU", e)
            return SW_COMMAND_NOT_ALLOWED
        }
    }

    private fun handleSelect(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        try {
            Log.d(TAG, "SELECT: P1=0x${p1.toString(16)}, P2=0x${p2.toString(16)}")

            // SELECT by AID (Application ID)
            if (p1 == P1_SELECT_BY_NAME) {
                if (apdu.size < 5) return SW_WRONG_LENGTH

                val aidLength = apdu[4].toInt() and 0xFF
                if (aidLength <= 0 || 5 + aidLength > apdu.size) {
                    return SW_WRONG_LENGTH
                }

                val aid = apdu.copyOfRange(5, 5 + aidLength)
                Log.d(TAG, "SELECT AID: ${bytesToHex(aid)}")

                // Check if it matches our NDEF AID
                if (aid.contentEquals(NDEF_AID)) {
                    Log.d(TAG, "NDEF application selected")
                    currentFile = 0 // Reset file selection
                    return SW_SUCCESS
                }

                Log.d(TAG, "Unknown AID")
                return SW_FILE_NOT_FOUND
            }

            // SELECT by File ID
            if (p1 == P1_SELECT_BY_FILE_ID) {
                if (apdu.size < 7) return SW_WRONG_LENGTH

                val fileIdHi = apdu[5].toInt() and 0xFF
                val fileIdLo = apdu[6].toInt() and 0xFF
                val fileId = (fileIdHi shl 8) or fileIdLo

                Log.d(TAG, "SELECT File ID: 0x${fileId.toString(16)}")

                when (fileId) {
                    FILE_ID_CC -> {
                        currentFile = 1
                        Log.d(TAG, "CC file selected")
                        return SW_SUCCESS
                    }
                    FILE_ID_NDEF -> {
                        currentFile = 2
                        Log.d(TAG, "NDEF file selected")
                        return SW_SUCCESS
                    }
                    else -> {
                        Log.d(TAG, "Unknown file ID")
                        return SW_FILE_NOT_FOUND
                    }
                }
            }

            Log.d(TAG, "Unsupported SELECT parameters")
            return SW_FILE_NOT_FOUND
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleSelect", e)
            return SW_FILE_NOT_FOUND
        }
    }

    private fun handleReadBinary(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        try {
            val offset = (p1 shl 8) or p2
            val le = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0

            Log.d(TAG, "READ BINARY: offset=$offset, le=$le, currentFile=$currentFile")

            return when (currentFile) {
                1 -> readFromCCFile(offset, le)
                2 -> readFromNdefFile(offset, le)
                else -> {
                    Log.d(TAG, "No file selected")
                    SW_FILE_NOT_FOUND
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleReadBinary", e)
            return SW_COMMAND_NOT_ALLOWED
        }
    }

    private fun readFromCCFile(offset: Int, le: Int): ByteArray {
        if (offset >= CC_FILE.size) {
            return SW_EOF
        }

        val available = CC_FILE.size - offset
        val length = if (le == 0) available else minOf(le, available)

        val response = CC_FILE.copyOfRange(offset, offset + length)
        Log.d(TAG, "Read $length bytes from CC file")

        return response + SW_SUCCESS
    }

    private fun readFromNdefFile(offset: Int, le: Int): ByteArray {
        if (ndefData == null) {
            Log.e(TAG, "NDEF data is null")
            return SW_FILE_NOT_FOUND
        }

        val data = ndefData!!

        if (offset >= data.size) {
            return SW_EOF
        }

        val available = data.size - offset
        val length = if (le == 0) available else minOf(le, available)

        val response = data.copyOfRange(offset, offset + length)
        Log.d(TAG, "Read $length bytes from NDEF file at offset $offset")

        return response + SW_SUCCESS
    }

    private fun handleUpdateBinary(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        Log.d(TAG, "UPDATE BINARY - write not supported")
        return SW_COMMAND_NOT_ALLOWED
    }

    private fun loadNdefData(uid: String): ByteArray {
        return try {
            val tag = repository.getTagByUid(uid)

            if (tag?.ndefMessage != null && tag.ndefMessage!!.isNotEmpty()) {
                Log.d(TAG, "Using saved NDEF message, type: ${tag.type}")

                // Log the first few bytes to verify format
                val message = tag.ndefMessage!!
                if (message.size >= 2) {
                    val ndefLength = ((message[0].toInt() and 0xFF) shl 8) or (message[1].toInt() and 0xFF)
                    Log.d(TAG, "NDEF message length field: $ndefLength, actual size: ${message.size}")
                }

                return message
            }

            Log.d(TAG, "Creating default NDEF message")
            createDefaultNdefMessage(uid)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading NDEF data", e)
            createDefaultNdefMessage(uid)
        }
    }

    private fun createDefaultNdefMessage(uid: String): ByteArray {
        // Create a proper NDEF message with a URL
        val url = "https://example.com/tag/$uid"

        // Create URI record
        val uriRecord = createNdefUriRecord(url)

        // Wrap with NLEN (2 bytes length)
        val message = ByteArray(2 + uriRecord.size)
        message[0] = ((uriRecord.size shr 8) and 0xFF).toByte()
        message[1] = (uriRecord.size and 0xFF).toByte()
        System.arraycopy(uriRecord, 0, message, 2, uriRecord.size)

        return message
    }

    private fun createNdefUriRecord(url: String): ByteArray {
        // NDEF URI Record format according to NFC Forum specification
        // Header: MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01 (NFC Forum well-known type)
        val header: Byte = 0xD1.toByte()

        // Type length = 1 (for "U")
        val typeLength: Byte = 0x01

        // Determine URI prefix code
        val (uriCode, cleanUrl) = getUriCodeAndCleanUrl(url)
        val urlBytes = cleanUrl.toByteArray(Charset.forName("UTF-8"))

        // Payload length = URI code (1) + URL bytes
        val payloadLength = (urlBytes.size + 1).toByte()

        // Type = 'U' (0x55)
        val type: Byte = 0x55.toByte()

        // Build record: Header (1) + Type Length (1) + Payload Length (1) + Type (1) + Payload
        val record = ByteArray(4 + 1 + urlBytes.size)
        var pos = 0
        record[pos++] = header
        record[pos++] = typeLength
        record[pos++] = payloadLength
        record[pos++] = type
        record[pos++] = uriCode
        urlBytes.forEach { record[pos++] = it }

        return record
    }

    private fun getUriCodeAndCleanUrl(url: String): Pair<Byte, String> {
        return when {
            url.startsWith("http://www.") -> Pair(0x01, url.substring(11))
            url.startsWith("https://www.") -> Pair(0x02, url.substring(12))
            url.startsWith("http://") -> Pair(0x03, url.substring(7))
            url.startsWith("https://") -> Pair(0x04, url.substring(8))
            url.startsWith("tel:") -> Pair(0x04, url.substring(4))
            url.startsWith("mailto:") -> Pair(0x05, url.substring(7))
            else -> Pair(0x00, url)
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: reason=$reason")
        currentFile = 0
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}