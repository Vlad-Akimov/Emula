package com.example.nfctagemulator.nfc.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import com.example.nfctagemulator.data.model.TagData

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
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
    )

    fun enable(activity: Activity) {
        if (adapter == null) {
            Log.e("NfcReader", "NFC не поддерживается")
            return
        }

        if (!adapter.isEnabled) {
            Log.e("NfcReader", "NFC выключен")
            return
        }

        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
            isEnabled = true
            Log.d("NfcReader", "Режим чтения ВКЛЮЧЕН")
        } catch (e: Exception) {
            Log.e("NfcReader", "Ошибка включения", e)
        }
    }

    fun disable(activity: Activity) {
        if (isEnabled) {
            try {
                adapter?.disableForegroundDispatch(activity)
                isEnabled = false
                Log.d("NfcReader", "Режим чтения ВЫКЛЮЧЕН")
            } catch (e: Exception) {
                Log.e("NfcReader", "Ошибка выключения", e)
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
        Log.d("NfcReader", "Прочитана метка: $uid")

        return TagData(uid = uid)
    }
}