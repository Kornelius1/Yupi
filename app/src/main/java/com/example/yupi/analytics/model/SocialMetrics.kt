package com.example.yupi.analytics.model

data class SocialMetrics(
    val ownerDurationMs: Long = 0L,
    val interlocutorDurationMs: Long = 0L,
    val turnCount: Int = 0,
    val dialogueDurationMs: Long = 0L,
    val monologueDurationMs: Long = 0L,
    val interactivityScore: Float = 0.0f
)