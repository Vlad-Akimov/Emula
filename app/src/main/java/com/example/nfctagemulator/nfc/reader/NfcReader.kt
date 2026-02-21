package com.example.nfctagemulator.nfc.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.util.Log
import com.example.nfctagemulator.data.model.*
import com.example.nfctagemulator.nfc.emulator.MifareClassicEmulator
import java.io.ByteArrayOutputStream

class NfcReader(private val context: Context) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var isEnabled = false

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, context.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val filters = arrayOf(
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
    )

    fun enable(activity: Activity) {
        if (adapter == null) {
            Log.e("NfcReader", "NFC is not supported")
            return
        }

        if (!adapter.isEnabled) {
            Log.e("NfcReader", "NFC is disabled")
            return
        }

        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
            isEnabled = true
            Log.d("NfcReader", "Reading mode enabled")
        } catch (e: Exception) {
            Log.e("NfcReader", "Activation error", e)
        }
    }

    fun disable(activity: Activity) {
        if (isEnabled) {
            try {
                adapter?.disableForegroundDispatch(activity)
                isEnabled = false
                Log.d("NfcReader", "Reading mode disabled")
            } catch (e: Exception) {
                Log.e("NfcReader", "Shutdown error", e)
            }
        }
    }

    fun readTag(intent: Intent): TagData? {
        val tag: Tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return null

        val uid = tag.id.joinToString("") { "%02X".format(it) }
        val techList = tag.techList?.toList() ?: emptyList()

        Log.d("NfcReader", "Tag detected: $uid")
        Log.d("NfcReader", "Tech list: $techList")

        // Try to detect card type and read data
        var ndefMessage: ByteArray? = null
        var mifareData: MifareData? = null
        var tagType = detectTagType(techList)

        // Try to read as MIFARE if applicable
        if (techList.contains(MifareClassic::class.java.name) ||
            techList.contains(MifareUltralight::class.java.name)) {
            // MifareDesfire is removed as it's not available in all Android versions

            mifareData = readMifareData(tag, tagType)

            // If it's a known special card type, update the type
            if (mifareData != null) {
                tagType = detectSpecialCardType(mifareData, uid) ?: tagType
            }
        }

        // Try to read NDEF if applicable
        if (techList.contains(Ndef::class.java.name) ||
            techList.contains(NdefFormatable::class.java.name)) {
            ndefMessage = readNdefMessage(tag)
            if (ndefMessage != null && tagType == TagType.UNKNOWN) {
                tagType = TagType.NDEF_URI // Default NDEF type
            }
        }

        return TagData(
            uid = uid,
            name = "Unnamed",
            type = tagType,
            rawData = tag.id,
            ndefMessage = ndefMessage,
            techList = techList,
            mifareData = mifareData
        )
    }

    private fun detectTagType(techList: List<String>): TagType {
        return when {
            techList.contains(MifareClassic::class.java.name) -> TagType.MIFARE_CLASSIC
            techList.contains(MifareUltralight::class.java.name) -> TagType.MIFARE_ULTRALIGHT
            techList.contains(IsoDep::class.java.name) -> TagType.ISO_DEP
            techList.contains(Ndef::class.java.name) ||
                    techList.contains(NdefFormatable::class.java.name) -> TagType.NDEF_URI
            else -> TagType.UNKNOWN
        }
    }

    private fun detectSpecialCardType(mifareData: MifareData?, uid: String): TagType? {
        if (mifareData == null) return null

        // Try to detect Troika card
        if (isTroikaCard(mifareData)) {
            return TagType.TROIKA
        }

        // Try to detect University card
        if (isUniversityCard(mifareData)) {
            return TagType.UNIVERSITY_CARD
        }

        return null
    }

    private fun isTroikaCard(mifareData: MifareData): Boolean {
        // Troika cards typically have specific patterns in sector 0
        val sector0 = mifareData.sectors[0]
        if (sector0 != null) {
            // Check for Troika-like data patterns
            val block1 = sector0.blocks[1]
            if (block1 != null && block1.size >= 4) {
                // Troika often has balance data in specific format
                if (block1[0] == 0x01.toByte() && (block1[3].toInt() and 0xFF) < 100) {
                    return true
                }
            }
        }
        return false
    }

    private fun isUniversityCard(mifareData: MifareData): Boolean {
        // Check for university card patterns
        val sector0 = mifareData.sectors[0]
        if (sector0 != null) {
            val block1 = sector0.blocks[1]
            if (block1 != null && block1.size >= 8) {
                // Check if block contains what looks like a student ID (numeric)
                // Convert bytes to string, ignoring non-printable characters
                val possibleId = StringBuilder()
                for (b in block1) {
                    if (b.toInt() >= 48 && b.toInt() <= 57) { // ASCII digits
                        possibleId.append(b.toChar())
                    }
                }
                if (possibleId.length >= 6) {
                    return true
                }
            }
        }
        return false
    }

    private fun readMifareData(tag: Tag, tagType: TagType): MifareData? {
        return when {
            tagType == TagType.MIFARE_CLASSIC -> readMifareClassic(tag)
            tagType == TagType.MIFARE_ULTRALIGHT -> readMifareUltralight(tag)
            else -> readGenericMifare(tag)
        }
    }

    private fun readMifareClassic(tag: Tag): MifareData? {
        try {
            val mifare = MifareClassic.get(tag) ?: return null
            mifare.connect()

            val sectors = mutableMapOf<Int, SectorData>()
            val keys = mutableMapOf<Int, KeyPair>()

            val sectorCount = mifare.sectorCount
            Log.d("NfcReader", "MIFARE Classic: $sectorCount sectors")

            // Try common keys
            val commonKeys = MifareClassicEmulator.DEFAULT_KEYS

            for (sector in 0 until sectorCount) {
                val blockCount = mifare.getBlockCountInSector(sector)
                val sectorBlocks = mutableMapOf<Int, ByteArray>()
                var authenticated = false
                var usedKey: ByteArray? = null

                // Try to authenticate with common keys
                for (key in commonKeys) {
                    try {
                        mifare.authenticateSectorWithKeyA(sector, key)
                        authenticated = true
                        usedKey = key
                        break
                    } catch (e: Exception) {
                        // Try next key
                    }
                }

                if (authenticated) {
                    Log.d("NfcReader", "Authenticated sector $sector")

                    // Read all blocks in sector
                    for (block in 0 until blockCount) {
                        val blockIndex = mifare.sectorToBlock(sector) + block
                        try {
                            val data = mifare.readBlock(blockIndex)
                            sectorBlocks[block] = data

                            // If it's the trailer block (last block), extract keys
                            if (block == blockCount - 1 && data.size >= 16) {
                                val keyA = data.copyOfRange(0, 6)
                                val keyB = data.copyOfRange(10, 16)
                                keys[sector] = KeyPair(keyA, keyB)
                            }
                        } catch (e: Exception) {
                            Log.e("NfcReader", "Error reading block $blockIndex", e)
                        }
                    }

                    sectors[sector] = SectorData(sectorBlocks, usedKey)
                } else {
                    Log.d("NfcReader", "Could not authenticate sector $sector")
                }
            }

            mifare.close()
            return MifareData(sectors = sectors, keys = keys)

        } catch (e: Exception) {
            Log.e("NfcReader", "Error reading MIFARE Classic", e)
            return null
        }
    }

    private fun readMifareUltralight(tag: Tag): MifareData? {
        try {
            val mifare = MifareUltralight.get(tag) ?: return null
            mifare.connect()

            val blocks = mutableMapOf<Int, ByteArray>()
            val pages = 64 // Ultralight has up to 64 pages (4 bytes per page)

            Log.d("NfcReader", "MIFARE Ultralight: reading pages")

            for (page in 0 until pages step 4) {
                try {
                    val data = mifare.readPages(page)
                    if (data.size >= 16) {
                        blocks[page] = data.copyOfRange(0, 4)
                        if (page + 1 < pages) blocks[page + 1] = data.copyOfRange(4, 8)
                        if (page + 2 < pages) blocks[page + 2] = data.copyOfRange(8, 12)
                        if (page + 3 < pages) blocks[page + 3] = data.copyOfRange(12, 16)
                    }
                } catch (e: Exception) {
                    Log.e("NfcReader", "Error reading page $page", e)
                    break
                }
            }

            mifare.close()
            return MifareData(blocks = blocks)

        } catch (e: Exception) {
            Log.e("NfcReader", "Error reading MIFARE Ultralight", e)
            return null
        }
    }

    private fun readGenericMifare(tag: Tag): MifareData? {
        // Try both Classic and Ultralight
        val classicData = readMifareClassic(tag)
        if (classicData != null) return classicData

        return readMifareUltralight(tag)
    }

    private fun readNdefMessage(tag: Tag): ByteArray? {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val message = ndef.ndefMessage
                ndef.close()

                if (message != null) {
                    return messageToByteArray(message)
                }
            }
        } catch (e: Exception) {
            Log.e("NfcReader", "Error reading NDEF", e)
        }
        return null
    }

    private fun messageToByteArray(message: NdefMessage): ByteArray {
        val baos = ByteArrayOutputStream()
        val records = message.records

        var totalSize = 0
        for (record in records) {
            totalSize += record.toByteArray().size
        }

        baos.write((totalSize shr 8) and 0xFF)
        baos.write(totalSize and 0xFF)

        for (record in records) {
            baos.write(record.toByteArray())
        }

        return baos.toByteArray()
    }
}