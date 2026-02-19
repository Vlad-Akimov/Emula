package com.example.nfctagemulator.nfc.emulator

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.nfctagemulator.data.repository.TagRepository

class TagHostApduService : HostApduService() {

    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator

    companion object {
        private const val TAG = "TagHostApduService"

        // Разные AID для разных типов меток
        private val AID_NDEF = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        private val AID_MIFARE = byteArrayOf(
            0xF2.toByte(), 0x22.toByte(), 0x22.toByte(), 0x22.toByte(), 0x22.toByte()
        )

        private val AID_TEXT = byteArrayOf(
            0xF1.toByte(), 0x23.toByte(), 0x45.toByte(), 0x67.toByte(), 0x89.toByte()
        )

        // Команды для Type 4 Tag
        private val SELECT_NDEF_APP = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76.toByte(), 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        private val SELECT_CC_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x03
        )

        private val SELECT_NDEF_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04
        )

        // Capability Container (CC) файл
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, // CCLEN = 15
            0x20, // Mapping Version 2.0
            0x00, 0x3B, // MLe = 59
            0x00, 0x34, // MLc = 52
            0x04, // NDEF
            0x06, // NDEF File Control TLV
            0xE1.toByte(), 0x04, // File ID
            0x02, 0x00, // Max NDEF Size = 512 байт
            0x00, // Read access
            0x00 // Write access
        )

        // Статусы ответов
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x86.toByte())
        private val SW_WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
        private val SW_SELECTED = byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    private var currentFile: Int = 0 // 0 - none, 1 - CC, 2 - NDEF

    override fun onCreate() {
        super.onCreate()
        repository = TagRepository(this)
        emulator = TagEmulator(this)
        Log.d(TAG, "TagHostApduService created")
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "Received APDU: ${bytesToHex(commandApdu)}")

        // Проверяем, эмулируем ли мы метку
        val emulatingUid = emulator.getEmulatingTagUid()
        if (emulatingUid == null) {
            Log.d(TAG, "No tag being emulated")
            return SW_FILE_NOT_FOUND
        }

        Log.d(TAG, "Emulating tag with UID: $emulatingUid")

        return when {
            isSelectCommand(commandApdu) -> handleSelectCommand(commandApdu)
            isReadBinaryCommand(commandApdu) -> handleReadBinaryCommand(commandApdu, emulatingUid)
            isUpdateBinaryCommand(commandApdu) -> handleUpdateBinaryCommand(commandApdu)
            else -> {
                Log.d(TAG, "Unknown command: ${bytesToHex(commandApdu)}")
                SW_COMMAND_NOT_ALLOWED
            }
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: reason=$reason")
        currentFile = 0
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
        if (apdu.size < 5) return SW_WRONG_LENGTH

        Log.d(TAG, "SELECT command: ${bytesToHex(apdu)}")

        return when {
            // SELECT NDEF App
            apdu.contentEquals(SELECT_NDEF_APP) -> {
                Log.d(TAG, "SELECT NDEF App")
                currentFile = 1
                SW_SUCCESS
            }

            // SELECT CC file
            apdu.contentEquals(SELECT_CC_FILE) -> {
                Log.d(TAG, "SELECT CC file")
                currentFile = 1
                SW_SUCCESS
            }

            // SELECT NDEF file
            apdu.contentEquals(SELECT_NDEF_FILE) -> {
                Log.d(TAG, "SELECT NDEF file")
                currentFile = 2
                SW_SUCCESS
            }

            else -> {
                Log.d(TAG, "Unknown SELECT")
                SW_FILE_NOT_FOUND
            }
        }
    }

    private fun handleReadBinaryCommand(apdu: ByteArray, uid: String): ByteArray {
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
                    response + SW_SUCCESS
                } else {
                    SW_FILE_NOT_FOUND
                }
            }

            2 -> {
                // Чтение NDEF файла с данными метки
                val ndefData = createNdefMessageWithUid(uid)
                if (offset < ndefData.size) {
                    val end = minOf(offset + le, ndefData.size)
                    val response = ndefData.copyOfRange(offset, end)
                    response + SW_SUCCESS
                } else {
                    SW_FILE_NOT_FOUND
                }
            }

            else -> {
                Log.d(TAG, "No file selected")
                SW_FILE_NOT_FOUND
            }
        }
    }

    private fun handleUpdateBinaryCommand(apdu: ByteArray): ByteArray {
        Log.d(TAG, "UPDATE BINARY command (write not supported)")
        return SW_COMMAND_NOT_ALLOWED
    }

    private fun createNdefMessageWithUid(uid: String): ByteArray {
        // Создаем NDEF сообщение с UID
        // Формат: [NLEN high][NLEN low][NDEF record]

        val text = "NFC Tag: $uid"
        val textBytes = text.toByteArray(Charsets.UTF_8)

        // NDEF Record: TNF=1 (NFC Forum well-known type), Type="T" (Text)
        val ndefRecord = mutableListOf<Byte>()
        ndefRecord.add(0xD1.toByte()) // MB=1, ME=1, SR=1, IL=0, TNF=1
        ndefRecord.add(0x01) // Type length
        ndefRecord.add((textBytes.size + 3).toByte()) // Payload length
        ndefRecord.add(0x54) // 'T'
        ndefRecord.add(0x02) // Status (UTF-8, length 2)
        ndefRecord.addAll("en".toByteArray().toList()) // Language
        ndefRecord.addAll(textBytes.toList()) // Text

        // Добавляем длину сообщения (2 байта, big-endian)
        val result = mutableListOf<Byte>()
        result.add(((ndefRecord.size shr 8) and 0xFF).toByte())
        result.add((ndefRecord.size and 0xFF).toByte())
        result.addAll(ndefRecord)

        return result.toByteArray()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}