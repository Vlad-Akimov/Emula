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

        // AID для эмуляции NFC метки (стандартный для NDEF)
        private val AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // Команда SELECT AID
        private val SELECT_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            AID.size.toByte(), *AID
        )

        // Статусы ответов
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x86.toByte())
        private val SW_WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
    }

    override fun onCreate() {
        super.onCreate()
        repository = TagRepository(this)
        emulator = TagEmulator(this)
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "Received APDU: ${bytesToHex(commandApdu)}")

        // Проверяем SELECT AID команду
        if (isSelectAidCommand(commandApdu)) {
            Log.d(TAG, "SELECT AID command received")

            // Проверяем, эмулируем ли мы какую-то метку
            val emulatingUid = emulator.getEmulatingTagUid()
            if (emulatingUid != null) {
                // Эмулируем метку
                Log.d(TAG, "Emulating tag: $emulatingUid")
                return SW_SUCCESS
            } else {
                // Ничего не эмулируем
                return SW_FILE_NOT_FOUND
            }
        }

        // Если мы эмулируем метку, обрабатываем команды чтения UID
        val emulatingUid = emulator.getEmulatingTagUid()
        if (emulatingUid != null) {
            return handleReadCommand(commandApdu, emulatingUid)
        }

        return SW_FILE_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
    }

    private fun isSelectAidCommand(apdu: ByteArray): Boolean {
        if (apdu.size < 5) return false

        // Проверяем CLA=0x00, INS=0xA4, P1=0x04, P2=0x00
        return apdu[0] == 0x00.toByte() &&
                apdu[1] == 0xA4.toByte() &&
                apdu[2] == 0x04.toByte() &&
                apdu[3] == 0x00.toByte()
    }

    private fun handleReadCommand(apdu: ByteArray, uid: String): ByteArray {
        // Проверяем команду GET UID (может отличаться в зависимости от типа метки)
        if (apdu.size >= 4 && apdu[0] == 0x00.toByte() && apdu[1] == 0xCA.toByte()) {
            // Возвращаем UID
            val uidBytes = hexStringToByteArray(uid)
            return uidBytes + SW_SUCCESS
        }

        // Другие команды чтения можно добавить позже

        return SW_COMMAND_NOT_ALLOWED
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) +
                    Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}