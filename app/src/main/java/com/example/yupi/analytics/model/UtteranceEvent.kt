package com.example.yupi.analytics.model

data class UtteranceEvent(
    val speaker: SpeakerType,
    val durationMs: Long,
    val wordCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)