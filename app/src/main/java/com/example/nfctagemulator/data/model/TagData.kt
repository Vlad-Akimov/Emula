package com.example.nfctagemulator.data.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

data class TagData(
    val uid: String,
    val name: String = "Без имени",
    val timestamp: Long = System.currentTimeMillis(),
    val type: TagType = TagType.UNKNOWN,
    val rawData: ByteArray? = null,
    val ndefMessage: ByteArray? = null,
    val techList: List<String> = emptyList()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "Без имени",
        parcel.readLong(),
        TagType.valueOf(parcel.readString() ?: TagType.UNKNOWN.name),
        parcel.readInt().takeIf { it >= 0 }?.let { parcel.createByteArray() },
        parcel.readInt().takeIf { it >= 0 }?.let { parcel.createByteArray() },
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(name)
        parcel.writeLong(timestamp)
        parcel.writeString(type.name)

        // Для rawData
        if (rawData == null) {
            parcel.writeInt(-1)
        } else {
            parcel.writeInt(rawData.size)
            parcel.writeByteArray(rawData)
        }

        // Для ndefMessage
        if (ndefMessage == null) {
            parcel.writeInt(-1)
        } else {
            parcel.writeInt(ndefMessage.size)
            parcel.writeByteArray(ndefMessage)
        }

        parcel.writeStringList(techList)
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagData

        if (uid != other.uid) return false
        if (name != other.name) return false
        if (timestamp != other.timestamp) return false
        if (type != other.type) return false
        if (!Arrays.equals(rawData, other.rawData)) return false
        if (!Arrays.equals(ndefMessage, other.ndefMessage)) return false
        if (techList != other.techList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (rawData?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (ndefMessage?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + techList.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<TagData> {
        override fun createFromParcel(parcel: Parcel): TagData {
            return TagData(parcel)
        }

        override fun newArray(size: Int): Array<TagData?> {
            return arrayOfNulls(size)
        }
    }
}

enum class TagType {
    NDEF_TEXT,
    NDEF_URI,
    NDEF_SMART_POSTER,
    NDEF_VCARD,
    MIFARE_CLASSIC,
    MIFARE_ULTRALIGHT,
    ISO_DEP,
    UNKNOWN
}