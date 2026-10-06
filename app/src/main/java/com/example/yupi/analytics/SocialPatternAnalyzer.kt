package com.example.yupi.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.yupi.analytics.model.*

class SocialPatternAnalyzer(
    private val maxDialogueGapMs: Long = 5000L,
    private val similarityThreshold: Float = 0.41f
) {

    private val _metrics = MutableStateFlow(SocialMetrics())
    val metrics: StateFlow<SocialMetrics> = _metrics.asStateFlow()

    private var lastEvent: UtteranceEvent? = null

    fun processUtterance(
        similarity: Float,
        durationMs: Long,
        wordCount: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val speaker = if (similarity >= similarityThreshold) {
            SpeakerType.OWNER
        } else {
            SpeakerType.INTERLOCUTOR
        }

        val currentEvent = UtteranceEvent(speaker, durationMs, wordCount, timestamp)
        val currentMetrics = _metrics.value

        var newOwnerDuration = currentMetrics.ownerDurationMs
        var newInterlocutorDuration = currentMetrics.interlocutorDurationMs
        var newTurnCount = currentMetrics.turnCount
        var newDialogueDuration = currentMetrics.dialogueDurationMs
        var newMonologueDuration = currentMetrics.monologueDurationMs

        if (speaker == SpeakerType.OWNER) {
            newOwnerDuration += durationMs
        } else {
            newInterlocutorDuration += durationMs
        }

        val previous = lastEvent
        if (previous != null) {
            val gapMs = currentEvent.timestamp - (previous.timestamp + previous.durationMs)

            if (previous.speaker != currentEvent.speaker && gapMs <= maxDialogueGapMs) {
                newTurnCount++
                newDialogueDuration += durationMs + previous.durationMs
            } else if (previous.speaker == SpeakerType.OWNER && currentEvent.speaker == SpeakerType.OWNER) {
                newMonologueDuration += durationMs
            }
        } else {
            if (speaker == SpeakerType.OWNER) {
                newMonologueDuration += durationMs
            }
        }

        val totalInteractionTimeMinutes = (newOwnerDuration + newInterlocutorDuration) / 60000.0f
        val calculatedInteractivity = if (totalInteractionTimeMinutes > 0f) {
            newTurnCount / totalInteractionTimeMinutes
        } else {
            0.0f
        }

        _metrics.value = currentMetrics.copy(
            ownerDurationMs = newOwnerDuration,
            interlocutorDurationMs = newInterlocutorDuration,
            turnCount = newTurnCount,
            dialogueDurationMs = newDialogueDuration,
            monologueDurationMs = newMonologueDuration,
            interactivityScore = calculatedInteractivity
        )

        lastEvent = currentEvent
    }

}