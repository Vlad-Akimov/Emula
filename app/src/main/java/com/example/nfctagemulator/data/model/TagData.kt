package com.example.nfctagemulator.data.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

data class TagData(
    val uid: String,
    val name: String = "No name",
    val timestamp: Long = System.currentTimeMillis(),
    val type: TagType = TagType.UNKNOWN,
    val rawData: ByteArray? = null,
    val ndefMessage: ByteArray? = null,
    val techList: List<String> = emptyList(),
    val mifareData: MifareData? = null,
    val customData: Map<String, String> = emptyMap()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "No name",
        parcel.readLong(),
        TagType.valueOf(parcel.readString() ?: TagType.UNKNOWN.name),
        parcel.readInt().takeIf { it >= 0 }?.let { parcel.createByteArray() },
        parcel.readInt().takeIf { it >= 0 }?.let { parcel.createByteArray() },
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readParcelable(MifareData::class.java.classLoader),
        parcel.readSerializable() as? Map<String, String> ?: emptyMap()
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
        parcel.writeParcelable(mifareData, flags)
        parcel.writeSerializable(customData as HashMap<String, String>)
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
        if (mifareData != other.mifareData) return false
        if (customData != other.customData) return false

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
        result = 31 * result + (mifareData?.hashCode() ?: 0)
        result = 31 * result + customData.hashCode()
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
    MIFARE_DESFIRE,
    ISO_DEP,
    TROIKA,
    UNIVERSITY_CARD,
    UNKNOWN
}

data class MifareData(
    val sectors: Map<Int, SectorData> = emptyMap(),
    val blocks: Map<Int, ByteArray> = emptyMap(),
    val keys: Map<Int, KeyPair> = emptyMap()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as? Map<Int, SectorData> ?: emptyMap(),
        parcel.readSerializable() as? Map<Int, ByteArray> ?: emptyMap(),
        parcel.readSerializable() as? Map<Int, KeyPair> ?: emptyMap()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(sectors as HashMap<Int, SectorData>)
        parcel.writeSerializable(blocks as HashMap<Int, ByteArray>)
        parcel.writeSerializable(keys as HashMap<Int, KeyPair>)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MifareData> {
        override fun createFromParcel(parcel: Parcel): MifareData {
            return MifareData(parcel)
        }

        override fun newArray(size: Int): Array<MifareData?> {
            return arrayOfNulls(size)
        }
    }
}

data class SectorData(
    val blocks: Map<Int, ByteArray> = emptyMap(),
    val sectorTrailer: ByteArray? = null
)

data class KeyPair(
    val keyA: ByteArray? = null,
    val keyB: ByteArray? = null
)