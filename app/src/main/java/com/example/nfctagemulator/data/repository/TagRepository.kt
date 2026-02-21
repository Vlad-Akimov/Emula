package com.example.nfctagemulator.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
import com.example.nfctagemulator.data.model.MifareData
import com.example.nfctagemulator.data.model.SectorData
import com.example.nfctagemulator.data.model.KeyPair
import org.json.JSONArray
import org.json.JSONObject

class TagRepository(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("nfc_tags", Context.MODE_PRIVATE)
    private val tagsKey = "saved_tags"

    fun saveTag(tag: TagData) {
        val tags = getAllTags().toMutableList()
        val existingIndex = tags.indexOfFirst { it.uid == tag.uid }

        if (existingIndex >= 0) {
            // Update existing tag, preserving name if new one is empty
            val newName = if (tag.name != "No name") tag.name else tags[existingIndex].name
            tags[existingIndex] = tag.copy(name = newName)
        } else {
            tags.add(tag)
        }

        saveTags(tags)
    }

    fun getAllTags(): List<TagData> {
        val tagsJson = sharedPrefs.getString(tagsKey, null) ?: return emptyList()
        return try {
            parseTagsFromJson(tagsJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deleteTag(uid: String) {
        val tags = getAllTags().toMutableList()
        tags.removeAll { it.uid == uid }
        saveTags(tags)
    }

    fun updateTagName(uid: String, newName: String) {
        val tags = getAllTags().toMutableList()
        val index = tags.indexOfFirst { it.uid == uid }
        if (index >= 0) {
            tags[index] = tags[index].copy(name = newName)
            saveTags(tags)
        }
    }

    fun getTagByUid(uid: String): TagData? {
        return getAllTags().find { it.uid == uid }
    }

    fun getTagsByType(type: TagType): List<TagData> {
        return getAllTags().filter { it.type == type }
    }

    private fun saveTags(tags: List<TagData>) {
        val tagsJson = convertTagsToJson(tags)
        sharedPrefs.edit().putString(tagsKey, tagsJson).apply()
    }

    private fun convertTagsToJson(tags: List<TagData>): String {
        val jsonArray = JSONArray()

        for (tag in tags) {
            val jsonObject = JSONObject()
            jsonObject.put("uid", tag.uid)
            jsonObject.put("name", tag.name)
            jsonObject.put("timestamp", tag.timestamp)
            jsonObject.put("type", tag.type.name)

            tag.rawData?.let {
                jsonObject.put("rawData", Base64.encodeToString(it, Base64.DEFAULT))
            }

            tag.ndefMessage?.let {
                jsonObject.put("ndefMessage", Base64.encodeToString(it, Base64.DEFAULT))
            }

            if (tag.techList.isNotEmpty()) {
                jsonObject.put("techList", JSONArray(tag.techList))
            }

            // Save Mifare data
            tag.mifareData?.let { mifareData ->
                val mifareJson = JSONObject()

                // Save sectors
                if (mifareData.sectors.isNotEmpty()) {
                    val sectorsJson = JSONObject()
                    mifareData.sectors.forEach { (sectorIdx, sectorData) ->
                        val sectorJson = JSONObject()

                        // Save blocks in sector
                        if (sectorData.blocks.isNotEmpty()) {
                            val blocksJson = JSONObject()
                            sectorData.blocks.forEach { (blockIdx, blockData) ->
                                blocksJson.put(blockIdx.toString(),
                                    Base64.encodeToString(blockData, Base64.DEFAULT))
                            }
                            sectorJson.put("blocks", blocksJson)
                        }

                        // Save sector trailer
                        sectorData.sectorTrailer?.let {
                            sectorJson.put("trailer",
                                Base64.encodeToString(it, Base64.DEFAULT))
                        }

                        sectorsJson.put(sectorIdx.toString(), sectorJson)
                    }
                    mifareJson.put("sectors", sectorsJson)
                }

                // Save keys
                if (mifareData.keys.isNotEmpty()) {
                    val keysJson = JSONObject()
                    mifareData.keys.forEach { (sectorIdx, keyPair) ->
                        val keyJson = JSONObject()
                        keyPair.keyA?.let {
                            keyJson.put("keyA", Base64.encodeToString(it, Base64.DEFAULT))
                        }
                        keyPair.keyB?.let {
                            keyJson.put("keyB", Base64.encodeToString(it, Base64.DEFAULT))
                        }
                        keysJson.put(sectorIdx.toString(), keyJson)
                    }
                    mifareJson.put("keys", keysJson)
                }

                // Save individual blocks (for Ultralight)
                if (mifareData.blocks.isNotEmpty()) {
                    val blocksJson = JSONObject()
                    mifareData.blocks.forEach { (blockIdx, blockData) ->
                        blocksJson.put(blockIdx.toString(),
                            Base64.encodeToString(blockData, Base64.DEFAULT))
                    }
                    mifareJson.put("blocks", blocksJson)
                }

                jsonObject.put("mifareData", mifareJson)
            }

            // Save custom data
            if (tag.customData.isNotEmpty()) {
                jsonObject.put("customData", JSONObject(tag.customData))
            }

            jsonArray.put(jsonObject)
        }

        return jsonArray.toString()
    }

    private fun parseTagsFromJson(json: String): List<TagData> {
        val tags = mutableListOf<TagData>()

        try {
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val uid = jsonObject.optString("uid", "")
                if (uid.isEmpty()) continue

                val name = jsonObject.optString("name", "Unnamed")
                val timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                val typeName = jsonObject.optString("type", TagType.UNKNOWN.name)
                val type = try {
                    TagType.valueOf(typeName)
                } catch (e: Exception) {
                    TagType.UNKNOWN
                }

                var rawData: ByteArray? = null
                if (jsonObject.has("rawData")) {
                    val rawDataStr = jsonObject.optString("rawData")
                    if (rawDataStr.isNotEmpty()) {
                        rawData = Base64.decode(rawDataStr, Base64.DEFAULT)
                    }
                }

                var ndefMessage: ByteArray? = null
                if (jsonObject.has("ndefMessage")) {
                    val ndefStr = jsonObject.optString("ndefMessage")
                    if (ndefStr.isNotEmpty()) {
                        ndefMessage = Base64.decode(ndefStr, Base64.DEFAULT)
                    }
                }

                val techList = mutableListOf<String>()
                if (jsonObject.has("techList")) {
                    val techArray = jsonObject.getJSONArray("techList")
                    for (j in 0 until techArray.length()) {
                        techList.add(techArray.getString(j))
                    }
                }

                // Parse Mifare data
                var mifareData: MifareData? = null
                if (jsonObject.has("mifareData")) {
                    val mifareJson = jsonObject.getJSONObject("mifareData")

                    val sectors = mutableMapOf<Int, SectorData>()
                    val blocks = mutableMapOf<Int, ByteArray>()
                    val keys = mutableMapOf<Int, KeyPair>()

                    // Parse sectors
                    if (mifareJson.has("sectors")) {
                        val sectorsJson = mifareJson.getJSONObject("sectors")
                        val sectorKeys = sectorsJson.keys()
                        while (sectorKeys.hasNext()) {
                            val sectorIdx = sectorKeys.next().toInt()
                            val sectorJson = sectorsJson.getJSONObject(sectorIdx.toString())

                            val sectorBlocks = mutableMapOf<Int, ByteArray>()
                            if (sectorJson.has("blocks")) {
                                val blocksJson = sectorJson.getJSONObject("blocks")
                                val blockKeys = blocksJson.keys()
                                while (blockKeys.hasNext()) {
                                    val blockIdx = blockKeys.next().toInt()
                                    val blockData = Base64.decode(
                                        blocksJson.getString(blockIdx.toString()),
                                        Base64.DEFAULT
                                    )
                                    sectorBlocks[blockIdx] = blockData
                                }
                            }

                            var trailer: ByteArray? = null
                            if (sectorJson.has("trailer")) {
                                trailer = Base64.decode(
                                    sectorJson.getString("trailer"),
                                    Base64.DEFAULT
                                )
                            }

                            sectors[sectorIdx] = SectorData(sectorBlocks, trailer)
                        }
                    }

                    // Parse keys
                    if (mifareJson.has("keys")) {
                        val keysJson = mifareJson.getJSONObject("keys")
                        val keySectors = keysJson.keys()
                        while (keySectors.hasNext()) {
                            val sectorIdx = keySectors.next().toInt()
                            val keyJson = keysJson.getJSONObject(sectorIdx.toString())

                            var keyA: ByteArray? = null
                            var keyB: ByteArray? = null

                            if (keyJson.has("keyA")) {
                                keyA = Base64.decode(keyJson.getString("keyA"), Base64.DEFAULT)
                            }
                            if (keyJson.has("keyB")) {
                                keyB = Base64.decode(keyJson.getString("keyB"), Base64.DEFAULT)
                            }

                            keys[sectorIdx] = KeyPair(keyA, keyB)
                        }
                    }

                    // Parse individual blocks
                    if (mifareJson.has("blocks")) {
                        val blocksJson = mifareJson.getJSONObject("blocks")
                        val blockIndices = blocksJson.keys()
                        while (blockIndices.hasNext()) {
                            val blockIdx = blockIndices.next().toInt()
                            val blockData = Base64.decode(
                                blocksJson.getString(blockIdx.toString()),
                                Base64.DEFAULT
                            )
                            blocks[blockIdx] = blockData
                        }
                    }

                    if (sectors.isNotEmpty() || blocks.isNotEmpty() || keys.isNotEmpty()) {
                        mifareData = MifareData(sectors, blocks, keys)
                    }
                }

                // Parse custom data
                val customData = mutableMapOf<String, String>()
                if (jsonObject.has("customData")) {
                    val customJson = jsonObject.getJSONObject("customData")
                    val keys = customJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        customData[key] = customJson.getString(key)
                    }
                }

                tags.add(TagData(
                    uid = uid,
                    name = name,
                    timestamp = timestamp,
                    type = type,
                    rawData = rawData,
                    ndefMessage = ndefMessage,
                    techList = techList,
                    mifareData = mifareData,
                    customData = customData
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tags
    }
}