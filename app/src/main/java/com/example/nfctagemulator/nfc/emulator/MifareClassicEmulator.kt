package com.example.nfctagemulator.nfc.emulator

import android.util.Log
import com.example.nfctagemulator.data.model.MifareData
import com.example.nfctagemulator.data.model.SectorData
import com.example.nfctagemulator.data.model.KeyPair
import com.example.nfctagemulator.data.model.TagType

class MifareClassicEmulator {

    companion object {
        private const val TAG = "MifareClassicEmulator"

        // MIFARE Classic commands
        private const val CMD_AUTH_A: Byte = 0x60
        private const val CMD_AUTH_B: Byte = 0x61
        private const val CMD_READ: Byte = 0x30
        private const val CMD_WRITE: Byte = 0xA0.toByte()
        private const val CMD_GET_UID: Byte = 0xFF.toByte() // Custom for emulation

        // Status codes
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_AUTH_FAILED = byteArrayOf(0x63, 0x00)
        private val SW_ACCESS_DENIED = byteArrayOf(0x69, 0x82.toByte())
        private val SW_WRITE_ERROR = byteArrayOf(0x65, 0x81.toByte())
        private val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69, 0x86.toByte())

        // Default keys for common cards
        val DEFAULT_KEYS = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), // Default
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()), // NFC Forum
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()), // MIFARE Classic
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // All zeros
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()), // Common
            byteArrayOf(0x4D, 0x3A, 0x99.toByte(), 0xC3.toByte(), 0x51, 0xDD.toByte()), // Troika specific?
            byteArrayOf(0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte(), 0xA6.toByte(), 0xA7.toByte())  // Another common
        )
    }

    private var currentSector = -1
    private var currentBlock = -1
    private var authenticated = false
    private var authKey: ByteArray? = null

    // Используем @Volatile для безопасного доступа из разных потоков
    @Volatile
    private var mifareData: MifareData? = null

    private var uid: ByteArray? = null
    private var tagType: TagType = TagType.UNKNOWN

    fun initialize(tagData: com.example.nfctagemulator.data.model.TagData) {
        // Локальная копия для безопасного присваивания
        val dataCopy = tagData.mifareData
        this.mifareData = dataCopy
        this.uid = tagData.rawData
        this.tagType = tagData.type
        authenticated = false
        currentSector = -1
        currentBlock = -1

        Log.d(TAG, "Initialized MIFARE emulator for type: ${tagData.type}")
    }

    fun processCommand(command: ByteArray): ByteArray {
        if (command.isEmpty()) return SW_COMMAND_NOT_ALLOWED

        val cmd = command[0]
        Log.d(TAG, "Processing MIFARE command: 0x${cmd.toString(16)}")

        return when (cmd.toInt() and 0xFF) {
            CMD_AUTH_A.toInt() and 0xFF, CMD_AUTH_B.toInt() and 0xFF ->
                handleAuthentication(command, cmd == CMD_AUTH_A)
            CMD_READ.toInt() and 0xFF -> handleRead(command)
            CMD_WRITE.toInt() and 0xFF -> handleWrite(command)
            CMD_GET_UID.toInt() and 0xFF -> handleGetUid()
            else -> {
                Log.d(TAG, "Unknown MIFARE command: 0x${cmd.toString(16)}")
                SW_COMMAND_NOT_ALLOWED
            }
        }
    }

    private fun handleAuthentication(command: ByteArray, useKeyA: Boolean): ByteArray {
        try {
            // Format: AUTH [sector] [key data...]
            if (command.size < 2) return SW_AUTH_FAILED

            val sector = command[1].toInt() and 0xFF
            val key = if (command.size > 2) {
                command.copyOfRange(2, minOf(8, command.size))
            } else null

            Log.d(TAG, "Authentication request for sector $sector, useKeyA=$useKeyA")

            // Создаем локальную копию для безопасного доступа
            val currentData = mifareData

            // Check if we have stored keys for this sector
            val sectorKey = getKeyForSector(sector, useKeyA, currentData)

            if (sectorKey != null && key != null) {
                if (sectorKey.contentEquals(key)) {
                    authenticated = true
                    currentSector = sector
                    Log.d(TAG, "Authentication successful for sector $sector")
                    return SW_SUCCESS
                }
            }

            // Try default keys if we don't have stored keys
            if (sectorKey == null) {
                for (defaultKey in DEFAULT_KEYS) {
                    if (key != null && defaultKey.contentEquals(key)) {
                        authenticated = true
                        currentSector = sector
                        Log.d(TAG, "Authentication successful with default key for sector $sector")
                        return SW_SUCCESS
                    }
                }
            }

            Log.d(TAG, "Authentication failed for sector $sector")
            authenticated = false
            return SW_AUTH_FAILED

        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
            return SW_AUTH_FAILED
        }
    }

    private fun handleRead(command: ByteArray): ByteArray {
        if (!authenticated) {
            Log.d(TAG, "Read attempted without authentication")
            return SW_ACCESS_DENIED
        }

        try {
            // Format: READ [block]
            if (command.size < 2) return SW_COMMAND_NOT_ALLOWED

            val block = command[1].toInt() and 0xFF
            val sector = block / 4 // 4 blocks per sector for Classic

            Log.d(TAG, "Read request for block $block, sector $sector")

            // Check if this block is in the current authenticated sector
            if (sector != currentSector) {
                Log.d(TAG, "Block $block is in different sector $sector, current $currentSector")
                return SW_ACCESS_DENIED
            }

            // Get block data
            val blockData = getBlockData(block)
            if (blockData != null) {
                Log.d(TAG, "Read successful, data size: ${blockData.size}")
                return blockData + SW_SUCCESS
            }

            // Return default data for this block type
            val defaultData = createDefaultBlockData(block)
            Log.d(TAG, "Returning default data for block $block")
            return defaultData + SW_SUCCESS

        } catch (e: Exception) {
            Log.e(TAG, "Read error", e)
            return SW_COMMAND_NOT_ALLOWED
        }
    }

    private fun handleWrite(command: ByteArray): ByteArray {
        if (!authenticated) {
            Log.d(TAG, "Write attempted without authentication")
            return SW_ACCESS_DENIED
        }

        try {
            // Format: WRITE [block] [data...]
            if (command.size < 2) return SW_COMMAND_NOT_ALLOWED

            val block = command[1].toInt() and 0xFF
            val sector = block / 4
            val data = if (command.size > 2) command.copyOfRange(2, command.size) else null

            Log.d(TAG, "Write request for block $block, data size: ${data?.size}")

            // Check if this block is in the current authenticated sector
            if (sector != currentSector) {
                Log.d(TAG, "Block $block is in different sector $sector")
                return SW_ACCESS_DENIED
            }

            // Check if it's a sector trailer block (last block of each sector)
            val isTrailerBlock = (block % 4 == 3)
            if (isTrailerBlock) {
                // For security, we might want to restrict writing to trailer blocks
                Log.d(TAG, "Write to trailer block $block")
            }

            if (data != null && data.size >= 16) {
                // In a real implementation, you would save this data
                // For now, just pretend it worked
                Log.d(TAG, "Write successful for block $block")
                return SW_SUCCESS
            }

            return SW_WRITE_ERROR

        } catch (e: Exception) {
            Log.e(TAG, "Write error", e)
            return SW_WRITE_ERROR
        }
    }

    private fun handleGetUid(): ByteArray {
        return if (uid != null) {
            uid!! + SW_SUCCESS
        } else {
            // Return a default UID if none exists
            byteArrayOf(0x04, 0x00, 0x00, 0x00) + SW_SUCCESS
        }
    }

    private fun getKeyForSector(sector: Int, useKeyA: Boolean, data: MifareData?): ByteArray? {
        val keys = data?.keys ?: return null
        val keyPair = keys[sector] ?: return null

        return if (useKeyA) keyPair.keyA else keyPair.keyB
    }

    private fun getBlockData(block: Int): ByteArray? {
        // Создаем локальную копию для безопасного доступа
        val currentData = mifareData ?: return null

        val sector = block / 4
        val blockInSector = block % 4

        val sectorData = currentData.sectors[sector]

        // Check if we have data for this specific block
        if (sectorData?.blocks?.containsKey(blockInSector) == true) {
            return sectorData.blocks[blockInSector]
        }

        // Check if we have data in the flat blocks map
        if (currentData.blocks.containsKey(block)) {
            return currentData.blocks[block]
        }

        // Check if we have sector trailer
        if (blockInSector == 3 && sectorData?.sectorTrailer != null) {
            return sectorData.sectorTrailer
        }

        return null
    }

    private fun createDefaultBlockData(block: Int): ByteArray {
        val sector = block / 4
        val blockInSector = block % 4

        return when (tagType) {
            TagType.TROIKA -> createTroikaBlockData(block, sector, blockInSector)
            TagType.UNIVERSITY_CARD -> createUniversityCardBlockData(block, sector, blockInSector)
            else -> createGenericBlockData(block, sector, blockInSector)
        }
    }

    private fun createTroikaBlockData(block: Int, sector: Int, blockInSector: Int): ByteArray {
        // Troika card specific default data pattern
        val data = ByteArray(16)

        when (sector) {
            0 -> {
                // Sector 0 contains manufacturer data and UID
                when (blockInSector) {
                    0 -> {
                        // Block 0: UID and manufacturer data
                        if (uid != null && uid!!.size >= 4) {
                            System.arraycopy(uid, 0, data, 0, minOf(4, uid!!.size))
                        } else {
                            // Default Troika-like UID
                            data[0] = 0x04
                            data[1] = 0x11
                            data[2] = 0x22
                            data[3] = 0x33
                        }
                        // Manufacturer data
                        data[4] = 0x08
                        data[5] = 0x04
                        data[6] = 0x00
                        data[7] = 0x02
                    }
                    1 -> {
                        // Block 1: Troika data block
                        data[0] = 0x01  // Transport type
                        data[1] = 0x00
                        data[2] = 0x00
                        data[3] = 0x64  // Balance low byte
                        data[4] = 0x00  // Balance high byte
                        data[5] = 0x00
                        data[6] = 0x00
                        data[7] = 0x00
                        data[8] = 0x01  // Trips remaining
                        data[9] = 0x00
                    }
                    2 -> {
                        // Block 2: More Troika data
                        data[0] = 0x00
                        data[1] = 0x00
                        data[2] = 0x01  // Date of last use
                        data[3] = 0x02
                        data[4] = 0x03
                    }
                    3 -> {
                        // Sector trailer for sector 0
                        data[0] = 0xA0.toByte()
                        data[1] = 0xA1.toByte()
                        data[2] = 0xA2.toByte()
                        data[3] = 0xA3.toByte()
                        data[4] = 0xA4.toByte()
                        data[5] = 0xA5.toByte()
                        data[6] = 0x78.toByte()  // Access conditions
                        data[7] = 0x77.toByte()
                        data[8] = 0x88.toByte()
                        data[9] = 0x69.toByte()
                        data[10] = 0xB0.toByte()
                        data[11] = 0xB1.toByte()
                        data[12] = 0xB2.toByte()
                        data[13] = 0xB3.toByte()
                        data[14] = 0xB4.toByte()
                        data[15] = 0xB5.toByte()
                    }
                }
            }
            else -> {
                // Other sectors have default patterns
                for (i in 0 until 16) {
                    data[i] = ((sector * 16 + blockInSector * 16 + i) % 256).toByte()
                }
            }
        }

        return data
    }

    private fun createUniversityCardBlockData(block: Int, sector: Int, blockInSector: Int): ByteArray {
        // University card specific data
        val data = ByteArray(16)

        when (sector) {
            0 -> {
                when (blockInSector) {
                    0 -> {
                        // UID block
                        if (uid != null && uid!!.size >= 4) {
                            System.arraycopy(uid, 0, data, 0, minOf(4, uid!!.size))
                        }
                        data[4] = 0x55  // University code
                        data[5] = 0xAA.toByte()
                    }
                    1 -> {
                        // Student ID
                        val studentId = "12345678".toByteArray()
                        System.arraycopy(studentId, 0, data, 0, minOf(8, studentId.size))
                    }
                    2 -> {
                        // Access permissions
                        data[0] = 0x01  // Building access
                        data[1] = 0x01  // Library access
                        data[2] = 0x01  // Lab access
                        data[3] = 0x00  // Other access
                    }
                    3 -> {
                        // Sector trailer
                        data[0] = 0x55.toByte()
                        data[1] = 0x55.toByte()
                        data[2] = 0x55.toByte()
                        data[3] = 0x55.toByte()
                        data[4] = 0x55.toByte()
                        data[5] = 0x55.toByte()
                        data[6] = 0x78.toByte()
                        data[7] = 0x77.toByte()
                        data[8] = 0x88.toByte()
                        data[9] = 0x69.toByte()
                        data[10] = 0xFF.toByte()
                        data[11] = 0xFF.toByte()
                        data[12] = 0xFF.toByte()
                        data[13] = 0xFF.toByte()
                        data[14] = 0xFF.toByte()
                        data[15] = 0xFF.toByte()
                    }
                }
            }
        }

        return data
    }

    private fun createGenericBlockData(block: Int, sector: Int, blockInSector: Int): ByteArray {
        val data = ByteArray(16)

        if (blockInSector == 3) {
            // Sector trailer - set some default keys and access bits
            // Key A
            data[0] = 0xFF.toByte()
            data[1] = 0xFF.toByte()
            data[2] = 0xFF.toByte()
            data[3] = 0xFF.toByte()
            data[4] = 0xFF.toByte()
            data[5] = 0xFF.toByte()

            // Access bits (typical transport configuration)
            data[6] = 0x78.toByte()  // 0x78 = 01111000
            data[7] = 0x77.toByte()  // 0x77 = 01110111
            data[8] = 0x88.toByte()  // 0x88 = 10001000

            // Key B
            data[9] = 0xFF.toByte()
            data[10] = 0xFF.toByte()
            data[11] = 0xFF.toByte()
            data[12] = 0xFF.toByte()
            data[13] = 0xFF.toByte()
            data[14] = 0xFF.toByte()
            data[15] = 0xFF.toByte()
        } else {
            // Data blocks - fill with pattern
            for (i in 0 until 16) {
                data[i] = ((block * 16 + i) % 256).toByte()
            }
        }

        return data
    }
}