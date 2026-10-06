package com.example.yupi.audio

import kotlin.math.sqrt

class SyllableDetector(
    private val sampleRate: Int = 16000
) {

    private val averageSyllablesPerWord = 2.1f

    private var minPeakDistanceMs = 100L

    fun countSyllablesInBuffer(
        audioSamples: ShortArray
    ): Int {

        if (audioSamples.isEmpty()) {
            return 0
        }

        var lastPeakTimeMs = -minPeakDistanceMs


        val floatBuffer =
            FloatArray(audioSamples.size)

        var sumSquares = 0.0

        for (i in audioSamples.indices) {
            val normalized =
                audioSamples[i] / 32768.0f

            floatBuffer[i] =
                normalized

            sumSquares +=
                normalized * normalized
        }

        val rms =
            sqrt(
                sumSquares /
                        audioSamples.size
            ).toFloat()

        if (rms < 0.02f) {
            return 0
        }

        var syllableCount = 0

        val frameSize = 256
        val hopSize = 128

        val adaptiveThreshold =
            rms * 1.05f

        var i = 0

        while (
            i + frameSize <=
            floatBuffer.size
        ) {

            var frameEnergy = 0.0f

            for (j in 0 until frameSize) {
                val sample =
                    floatBuffer[i + j]

                frameEnergy +=
                    sample * sample
            }

            frameEnergy =
                sqrt(
                    frameEnergy / frameSize
                )

            val currentTimeMs =
                (i.toLong() * 1000L) /
                        sampleRate

            if (
                frameEnergy > adaptiveThreshold &&
                currentTimeMs -
                lastPeakTimeMs >=
                minPeakDistanceMs
            ) {
                syllableCount++

                lastPeakTimeMs =
                    currentTimeMs
            }

            i += hopSize
        }

        return syllableCount
    }

    fun estimateWordCount(
        syllableCount: Int
    ): Int {

        if (syllableCount <= 0) {
            return 0
        }

        return Math.max(
            1,
            Math.round(
                syllableCount /
                        averageSyllablesPerWord
            )
        )
    }
}