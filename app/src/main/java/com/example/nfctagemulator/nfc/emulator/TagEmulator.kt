package com.example.nfctagemulator.nfc.emulator

import android.content.Context
import android.content.SharedPreferences
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.nfctagemulator.data.model.TagData
import java.io.IOException

class TagEmulator(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nfc_emulation", Context.MODE_PRIVATE)

    companion object {
        private const val EMULATING_TAG_UID_KEY = "emulating_tag_uid"
    }

    fun setEmulatingTag(tag: TagData?) {
        if (tag == null) {
            prefs.edit().remove(EMULATING_TAG_UID_KEY).apply()
        } else {
            prefs.edit().putString(EMULATING_TAG_UID_KEY, tag.uid).apply()
        }
    }

    fun getEmulatingTagUid(): String? {
        return prefs.getString(EMULATING_TAG_UID_KEY, null)
    }

    fun isEmulating(): Boolean {
        return getEmulatingTagUid() != null
    }

    // Метод для записи данных на метку (если нужно будет)
    fun writeToNfcTag(tag: Tag, tagData: TagData): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()

                // Создаем NDEF сообщение с UID метки
                val message = createNdefMessage(tagData)
                if (ndef.maxSize < message.toByteArray().size) {
                    return false
                }

                ndef.writeNdefMessage(message)
                ndef.close()
                return true
            } else {
                // Метка может быть форматируемой
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(createNdefMessage(tagData))
                    ndefFormatable.close()
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FormatException) {
            e.printStackTrace()
        }

        return false
    }

    private fun createNdefMessage(tagData: TagData): NdefMessage {
        // Создаем текстовую запись с именем метки
        val textRecord = NdefRecord.createTextRecord("en", tagData.name)

        // Создаем запись с UID (как URI)
        val uriRecord = NdefRecord.createUri("nfc://uid/${tagData.uid}")

        return NdefMessage(arrayOf(textRecord, uriRecord))
    }
}