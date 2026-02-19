package com.example.nfctagemulator.nfc.emulator

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.nfctagemulator.data.model.TagData

class TagEmulator(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nfc_emulation", Context.MODE_PRIVATE)

    companion object {
        private const val EMULATING_TAG_UID_KEY = "emulating_tag_uid"
    }

    fun setEmulatingTag(tag: TagData?) {
        val editor = prefs.edit()
        if (tag == null) {
            editor.remove(EMULATING_TAG_UID_KEY)
            Log.d("TagEmulator", "Эмуляция остановлена")
        } else {
            editor.putString(EMULATING_TAG_UID_KEY, tag.uid)
            Log.d("TagEmulator", "Эмуляция запущена: ${tag.uid}")
        }
        editor.apply()
    }

    fun getEmulatingTagUid(): String? {
        return prefs.getString(EMULATING_TAG_UID_KEY, null)
    }

    fun isEmulating(): Boolean {
        return getEmulatingTagUid() != null
    }
}