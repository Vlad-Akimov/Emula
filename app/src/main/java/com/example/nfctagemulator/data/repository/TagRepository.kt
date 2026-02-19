package com.example.nfctagemulator.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.model.TagType
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
            // Обновляем существующую метку, сохраняя имя если оно не пустое
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

                val name = jsonObject.optString("name", "Без имени")
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

                tags.add(TagData(
                    uid = uid,
                    name = name,
                    timestamp = timestamp,
                    type = type,
                    rawData = rawData,
                    ndefMessage = ndefMessage,
                    techList = techList
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tags
    }
}