package com.example.yupi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_word_logs")
data class WordLogEntity(
    @PrimaryKey
    val date: String,

    val wordCount: Int,

    val isSynced: Boolean = false
)