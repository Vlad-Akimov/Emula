package com.example.nfctagemulator.nfc.reader

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.example.nfctagemulator.data.model.TagData

class NfcReader(private val context: Context) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private val pendingIntent: PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, context.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

    private val filters = arrayOf(
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
    )

    fun enable(activity: android.app.Activity) {
        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disable(activity: android.app.Activity) {
        adapter?.disableForegroundDispatch(activity)
    }

    fun readTag(intent: Intent): TagData? {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null

        val uid = tag.id.joinToString("") {
            "%02X".format(it)
        }

        return TagData(uid = uid)
    }
}