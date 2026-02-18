package com.example.nfctagemulator.data.model

data class TagData(
    val uid: String,
    val name: String = "Без имени",
    val timestamp: Long = System.currentTimeMillis()
)