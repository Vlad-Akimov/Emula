package com.example.nfctagemulator.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nfctagemulator.data.model.TagData

class TagRepository(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("nfc_tags", Context.MODE_PRIVATE)
    private val tagsKey = "saved_tags"

    fun saveTag(tag: TagData) {
        val tags = getAllTags().toMutableList()
        val existingIndex = tags.indexOfFirst { it.uid == tag.uid }

        if (existingIndex >= 0) {
            tags[existingIndex] = tags[existingIndex].copy(name = tag.name)
        } else {
            tags.add(tag)
        }

        saveTags(tags)
    }

    fun getAllTags(): List<TagData> {
        val tagsJson = sharedPrefs.getString(tagsKey, null) ?: return emptyList()
        return parseTagsFromJson(tagsJson)
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

    private fun saveTags(tags: List<TagData>) {
        val tagsJson = convertTagsToJson(tags)
        sharedPrefs.edit().putString(tagsKey, tagsJson).apply()
    }

    // Простой парсинг без Gson
    private fun convertTagsToJson(tags: List<TagData>): String {
        val sb = StringBuilder()
        sb.append("[")
        for (i in tags.indices) {
            val tag = tags[i]
            sb.append("{\"uid\":\"${tag.uid}\",\"name\":\"${tag.name}\",\"timestamp\":${tag.timestamp}}")
            if (i < tags.size - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseTagsFromJson(json: String): List<TagData> {
        val tags = mutableListOf<TagData>()
        if (json == "[]") return tags

        try {
            // Убираем квадратные скобки
            val content = json.substring(1, json.length - 1)
            if (content.isEmpty()) return tags

            // Разбиваем по объектам
            val objects = content.split("},{")

            for (obj in objects) {
                var cleanObj = obj
                if (!cleanObj.startsWith("{")) cleanObj = "{$cleanObj"
                if (!cleanObj.endsWith("}")) cleanObj = "$cleanObj}"

                val uid = extractValue(cleanObj, "uid")
                val name = extractValue(cleanObj, "name")
                val timestamp = extractLongValue(cleanObj, "timestamp")

                if (uid != null) {
                    tags.add(TagData(
                        uid = uid,
                        name = name ?: "Без имени",
                        timestamp = timestamp ?: System.currentTimeMillis()
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tags
    }

    private fun extractValue(json: String, key: String): String? {
        val pattern = "\"$key\":\"([^\"]*)\""
        val regex = Regex(pattern)
        val matchResult = regex.find(json)
        return matchResult?.groupValues?.get(1)
    }

    private fun extractLongValue(json: String, key: String): Long? {
        val pattern = "\"$key\":(\\d+)"
        val regex = Regex(pattern)
        val matchResult = regex.find(json)
        return matchResult?.groupValues?.get(1)?.toLongOrNull()
    }
}