package com.example.nfctagemulator.nfc.emulator

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.nfctagemulator.data.repository.TagRepository
import java.nio.ByteBuffer
import java.nio.charset.Charset

class TagHostApduService : HostApduService() {

    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator

    companion object {
        private const val TAG = "TagHostApduService"

        // Статусы ответов ISO 7816
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69, 0x86.toByte())
        private val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        private val SW_EOF = byteArrayOf(0x62, 0x82.toByte())
        private val SW_SELECTED = byteArrayOf(0x90.toByte(), 0x00)

        // Capability Container (CC) файл для Type 4 Tag
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,                   // CCLEN = 15
            0x20,                          // Mapping Version 2.0
            0x00, 0x3F,                    // MLe = 63
            0x00, 0x3F,                    // MLc = 63
            0x04,                          // NDEF
            0x06,                          // NDEF File Control TLV
            (0xE1).toByte(), 0x04,           // File ID = 0xE104
            0x08, 0x00,                    // Max NDEF Size = 2048 байт
            0x00,                          // Read access
            0x00                            // Write access
        )

        // NDEF File Control TLV
        private val NDEF_FILE_CONTROL = byteArrayOf(
            (0xE1).toByte(), 0x04,           // File ID = 0xE104
            0x08, 0x00,                    // Max NDEF Size = 2048 байт
            0x00,                          // Read access
            0x00                            // Write access
        )

        // Команда SELECT NDEF App (стандартная)
        private val SELECT_NDEF_APP = byteArrayOf(
            0x00, (0xA4).toByte(), 0x04, 0x00, 0x07,
            (0xD2).toByte(), 0x76, 0x00, 0x00, (0x85).toByte(), 0x01, 0x01
        )
    }

    private var currentFile: Int = 0 // 0 - none, 1 - CC, 2 - NDEF
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

            // Проверяем, эмулируем ли мы метку
            val emulatingUid = emulator.getEmulatingTagUid()
            if (emulatingUid == null) {
                Log.d(TAG, "No tag being emulated")
                return SW_FILE_NOT_FOUND
            }

            // Обновляем UID если изменился
            if (currentUid != emulatingUid) {
                currentUid = emulatingUid
                ndefData = null // Сбросим кэш при смене метки
                Log.d(TAG, "Switched to emulating tag: $emulatingUid")
            }

            // Загружаем данные метки из репозитория
            if (ndefData == null) {
                ndefData = loadNdefData(emulatingUid)
            }

            return when {
                isSelectCommand(commandApdu) -> handleSelectCommand(commandApdu)
                isReadBinaryCommand(commandApdu) -> handleReadBinaryCommand(commandApdu)
                isUpdateBinaryCommand(commandApdu) -> handleUpdateBinaryCommand(commandApdu)
                else -> {
                    Log.d(TAG, "Unknown command: ${bytesToHex(commandApdu)}")
                    SW_COMMAND_NOT_ALLOWED
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing APDU", e)
            return SW_COMMAND_NOT_ALLOWED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: reason=$reason")
        currentFile = 0
        // Не сбрасываем ndefData и currentUid полностью, чтобы при следующей активации было быстрее
    }

    private fun isSelectCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
                apdu[0] == 0x00.toByte() &&
                apdu[1] == 0xA4.toByte()
    }

    private fun isReadBinaryCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
                apdu[0] == 0x00.toByte() &&
                apdu[1] == 0xB0.toByte()
    }

    private fun isUpdateBinaryCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 4 &&
                apdu[0] == 0x00.toByte() &&
                apdu[1] == 0xD6.toByte()
    }

    private fun handleSelectCommand(apdu: ByteArray): ByteArray {
        try {
            if (apdu.size < 4) return SW_WRONG_LENGTH

            val p1 = apdu[2].toInt() and 0xFF
            val p2 = apdu[3].toInt() and 0xFF

            Log.d(TAG, "SELECT command: P1=$p1, P2=$p2")

            // SELECT by name (AID)
            if (p1 == 0x04 && apdu.size > 5) {
                val aidLength = apdu[4].toInt() and 0xFF
                if (aidLength > 0 && 5 + aidLength <= apdu.size) {
                    val aid = apdu.copyOfRange(5, 5 + aidLength)
                    Log.d(TAG, "SELECT AID: ${bytesToHex(aid)}")

                    // Принимаем любой AID (для совместимости)
                    currentFile = 1
                    return SW_SUCCESS
                }
            }

            // SELECT by file ID (обычно P1=0x00, P2=0x0C)
            if (p1 == 0x00 && apdu.size >= 7) {
                val fileIdHi = apdu[5].toInt() and 0xFF
                val fileIdLo = apdu[6].toInt() and 0xFF
                val fileId = (fileIdHi shl 8) or fileIdLo

                Log.d(TAG, "SELECT file: 0x${Integer.toHexString(fileId)}")

                when (fileId) {
                    0xE103 -> { // CC file
                        currentFile = 1
                        return SW_SUCCESS
                    }
                    0xE104 -> { // NDEF file
                        currentFile = 2
                        return SW_SUCCESS
                    }
                }
            }

            // Если это команда SELECT NDEF App
            if (apdu.size >= 12 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()) {
                if (apdu[4] == 0x07.toByte()) { // Длина AID = 7
                    val aid = apdu.copyOfRange(5, 12)
                    Log.d(TAG, "Possible NDEF AID: ${bytesToHex(aid)}")
                    currentFile = 1
                    return SW_SUCCESS
                }
            }

            Log.d(TAG, "Unknown SELECT command")
            return SW_FILE_NOT_FOUND

        } catch (e: Exception) {
            Log.e(TAG, "Error in handleSelectCommand", e)
            return SW_FILE_NOT_FOUND
        }
    }

    private fun handleReadBinaryCommand(apdu: ByteArray): ByteArray {
        try {
            if (apdu.size < 5) return SW_WRONG_LENGTH

            val p1 = apdu[2].toInt() and 0xFF
            val p2 = apdu[3].toInt() and 0xFF
            val le = apdu[4].toInt() and 0xFF

            val offset = (p1 shl 8) or p2

            Log.d(TAG, "READ BINARY: offset=$offset, length=$le, currentFile=$currentFile")

            return when (currentFile) {
                1 -> {
                    // Чтение CC файла
                    if (offset < CC_FILE.size) {
                        val end = minOf(offset + le, CC_FILE.size)
                        val response = CC_FILE.copyOfRange(offset, end)
                        // Добавляем статусный байт
                        return response + SW_SUCCESS
                    } else {
                        SW_EOF
                    }
                }

                2 -> {
                    // Чтение NDEF файла с данными метки
                    if (ndefData == null) {
                        Log.e(TAG, "NDEF data is null")
                        return SW_FILE_NOT_FOUND
                    }

                    ndefData?.let { data ->
                        if (offset < data.size) {
                            val end = minOf(offset + le, data.size)
                            val response = data.copyOfRange(offset, end)
                            Log.d(TAG, "Returning ${response.size} bytes from NDEF file")
                            return response + SW_SUCCESS
                        } else {
                            Log.d(TAG, "EOF: offset=$offset >= data.size=${data.size}")
                            return SW_EOF
                        }
                    } ?: SW_FILE_NOT_FOUND
                }

                else -> {
                    Log.d(TAG, "No file selected")
                    SW_FILE_NOT_FOUND
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleReadBinaryCommand", e)
            return SW_COMMAND_NOT_ALLOWED
        }
    }

    private fun handleUpdateBinaryCommand(apdu: ByteArray): ByteArray {
        Log.d(TAG, "UPDATE BINARY command (write not supported)")
        return SW_COMMAND_NOT_ALLOWED
    }

    private fun loadNdefData(uid: String): ByteArray {
        try {
            // Пытаемся получить сохраненную метку из репозитория
            val tag = repository.getTagByUid(uid)

            // Если есть сохраненное NDEF сообщение - используем его
            if (tag?.ndefMessage != null && tag.ndefMessage!!.isNotEmpty()) {
                Log.d(TAG, "Using saved NDEF message from repository, size: ${tag.ndefMessage!!.size}")
                return tag.ndefMessage!!
            }

            // Иначе создаем стандартное сообщение с URL
            Log.d(TAG, "Creating default NDEF message for UID: $uid")
            return createDefaultNdefMessage(uid)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading NDEF data", e)
            return createDefaultNdefMessage(uid)
        }
    }

    private fun createDefaultNdefMessage(uid: String): ByteArray {
        // Создаем NDEF сообщение с URL, содержащим UID метки
        val url = "https://example.com/tag/$uid"
        val ndefRecord = createNdefUrlRecord(url)

        // Формат Type 4 Tag: [NLEN][NDEF Message]
        // NLEN = 2 байта, big-endian
        val result = ByteBuffer.allocate(2 + ndefRecord.size)
        result.putShort(ndefRecord.size.toShort())
        result.put(ndefRecord)

        return result.array()
    }

    private fun createNdefUrlRecord(url: String): ByteArray {
        try {
            // Создаем NDEF запись типа URL (URI)
            // TNF = 0x01 (NFC Forum well-known type), Type = "U" (URI)

            val urlBytes = url.toByteArray(Charset.forName("UTF-8"))

            // URI Identifier Code
            val uriCode: Byte = when {
                url.startsWith("http://www.") -> 0x01
                url.startsWith("https://www.") -> 0x02
                url.startsWith("http://") -> 0x03
                url.startsWith("https://") -> 0x04
                else -> 0x00
            }

            // Убираем префикс если он есть
            val cleanUrl = when (uriCode) {
                0x01.toByte() -> url.substring(11) // "http://www."
                0x02.toByte() -> url.substring(12) // "https://www."
                0x03.toByte() -> url.substring(7)  // "http://"
                0x04.toByte() -> url.substring(8)  // "https://"
                else -> url
            }

            val cleanUrlBytes = cleanUrl.toByteArray(Charset.forName("UTF-8"))

            // Создаем NDEF запись
            val record = ByteBuffer.allocate(4 + cleanUrlBytes.size)

            // Header: MB=1, ME=1, SR=1, IL=0, TNF=0x01
            record.put(0xD1.toByte())

            // Type length = 1
            record.put(0x01)

            // Payload length (URI code + URL)
            record.put((cleanUrlBytes.size + 1).toByte())

            // Type = 'U' (URI)
            record.put(0x55.toByte())

            // URI Identifier Code
            record.put(uriCode)

            // URI
            record.put(cleanUrlBytes)

            return record.array()

        } catch (e: Exception) {
            Log.e(TAG, "Error creating NDEF URL record", e)
            // Возвращаем минимальное валидное NDEF сообщение в случае ошибки
            return byteArrayOf(
                0xD1.toByte(), 0x01, 0x01, 0x55.toByte(), 0x00
            )
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}