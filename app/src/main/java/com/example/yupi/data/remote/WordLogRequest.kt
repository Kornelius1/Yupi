package com.example.yupi.data.remote

data class WordLogRequest(
    val deviceId: String,
    val date: String,
    val wordCount: Int
)