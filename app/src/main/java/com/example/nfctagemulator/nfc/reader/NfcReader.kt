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
import android.nfc.tech.Ndef
import android.util.Log
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

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
            Log.d("NfcReader", "Reading mode IS ENABLED")
        } catch (e: Exception) {
            Log.e("NfcReader", "Activation error", e)
        }
    }

    fun disable(activity: Activity) {
        if (isEnabled) {
            try {
                adapter?.disableForegroundDispatch(activity)
                isEnabled = false
                Log.d("NfcReader", "Reading mode is DISABLED")
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

        // Получаем список технологий
        val techList = tag.techList?.toList() ?: emptyList()

        // Пытаемся прочитать NDEF данные
        var ndefMessage: ByteArray? = null
        var tagType = TagType.UNKNOWN
        var contactName: String? = null
        var contactPhone: String? = null
        var contactEmail: String? = null

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val message = ndef.ndefMessage
                if (message != null) {
                    ndefMessage = messageToByteArray(message)

                    // Определяем тип по первой записи
                    if (message.records.isNotEmpty()) {
                        val firstRecord = message.records[0]
                        tagType = detectNdefType(firstRecord)

                        // Если это vCard, пытаемся извлечь данные
                        if (tagType == TagType.NDEF_VCARD) {
                            val vcardData = parseVCard(firstRecord)
                            contactName = vcardData.first
                            contactPhone = vcardData.second
                            contactEmail = vcardData.third
                        }
                    }
                }
                ndef.close()
            }
        } catch (e: Exception) {
            Log.e("NfcReader", "NDEF reading error", e)
        }

        Log.d("NfcReader", "The label has been read: $uid, type: $tagType")

        return TagData(
            uid = uid,
            type = tagType,
            rawData = tag.id,
            ndefMessage = ndefMessage,
            techList = techList,
            contactName = contactName,
            contactPhone = contactPhone,
            contactEmail = contactEmail
        )
    }

    private fun messageToByteArray(message: NdefMessage): ByteArray {
        val baos = ByteArrayOutputStream()
        val records = message.records

        // Формат: [NLEN][NDEF Records]
        // Сначала вычисляем общий размер
        var totalSize = 0
        for (record in records) {
            totalSize += record.toByteArray().size
        }

        // Записываем длину (2 байта, big-endian)
        baos.write((totalSize shr 8) and 0xFF)
        baos.write(totalSize and 0xFF)

        // Записываем записи
        for (record in records) {
            baos.write(record.toByteArray())
        }

        return baos.toByteArray()
    }

    private fun detectNdefType(record: NdefRecord): TagType {
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                val typeStr = String(record.type, Charsets.US_ASCII)
                when (typeStr) {
                    "T" -> TagType.NDEF_TEXT
                    "U" -> TagType.NDEF_URI
                    "Sp" -> TagType.NDEF_SMART_POSTER
                    else -> TagType.UNKNOWN
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val typeStr = String(record.type, Charsets.US_ASCII)
                if (typeStr.contains("vcard") || typeStr.contains("vcf")) {
                    TagType.NDEF_VCARD
                } else {
                    TagType.UNKNOWN
                }
            }
            else -> TagType.UNKNOWN
        }
    }

    private fun parseVCard(record: NdefRecord): Triple<String?, String?, String?> {
        try {
            val payload = record.payload
            val content = String(payload, Charset.forName("UTF-8"))

            var name: String? = null
            var phone: String? = null
            var email: String? = null

            val lines = content.split("\n")
            for (line in lines) {
                when {
                    line.startsWith("FN:") -> name = line.substring(3)
                    line.startsWith("TEL:") -> phone = line.substring(4)
                    line.startsWith("EMAIL:") -> email = line.substring(6)
                }
            }

            return Triple(name, phone, email)
        } catch (e: Exception) {
            Log.e("NfcReader", "Error parsing vCard", e)
            return Triple(null, null, null)
        }
    }
}