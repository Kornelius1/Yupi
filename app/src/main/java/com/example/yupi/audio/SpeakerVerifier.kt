package com.example.yupi.audio

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class SpeakerVerifier(context: Context) {

    private val env: OrtEnvironment =
        OrtEnvironment.getEnvironment()


    private val session: OrtSession

    private val hammingWindow = FloatArray(400) {
        (0.54 - 0.46 * cos(2.0 * Math.PI * it / 399)).toFloat()
    }
    private val melBank = createMelFilters(16000, 400, 80)
    private val cosTable = DoubleArray(400) { cos(2.0 * Math.PI * it / 400) }
    private val sinTable = DoubleArray(400) { sin(2.0 * Math.PI * it / 400) }

    init {
        val modelBytes =
            context.assets
                .open("ecapa_tdnn.onnx")
                .readBytes()

        session =
            env.createSession(
                modelBytes,
                OrtSession.SessionOptions()
            )
    }

    fun extractEmbedding(
        audioSamples: ShortArray
    ): FloatArray {

        val floatAudio =
            FloatArray(audioSamples.size)

        for (i in audioSamples.indices) {
            floatAudio[i] =
                audioSamples[i] / 32768.0f
        }

        val features =
            extractFbankFeatures(floatAudio)

        val frames = features.size / 80

        val inputShape =
            longArrayOf(
                1,
                frames.toLong(),
                80
            )

        val inputTensor =
            OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(features),
                inputShape
            )

        val inputs =
            mapOf(
                "feats" to inputTensor
            )

        val results =
            session.run(inputs)

        val output =
            results[0].value as Array<Array<FloatArray>>

        val embedding =
            output[0][0]

        inputTensor.close()
        results.close()

        return embedding
    }

    private fun extractFbankFeatures(
        audio: FloatArray
    ): FloatArray {

        val sampleRate = 16000
        val nFft = 400
        val frameLength = 400
        val hopLength = 160
        val nMels = 80

        val frameCount =
            1 + (audio.size + hopLength - 1) / hopLength

        val output =
            FloatArray(frameCount * nMels)



        val spectrum =
            FloatArray(nFft / 2 + 1)

        for (frame in 0 until frameCount) {

            val start =
                frame * hopLength -
                        frameLength / 2

            val windowed = FloatArray(frameLength)
            for (n in 0 until frameLength) {
                val index = start + n
                windowed[n] =
                    if (index >= 0 && index < audio.size) audio[index] * hammingWindow[n] else 0f
            }

            for (k in spectrum.indices) {
                var real = 0.0
                var imag = 0.0
                var idx = 0
                for (n in 0 until frameLength) {
                    val v = windowed[n]
                    real += v * cosTable[idx]
                    imag -= v * sinTable[idx]
                    idx += k
                    if (idx >= nFft) idx -= nFft
                }
                spectrum[k] = (real * real + imag * imag).toFloat()
            }

            for (mel in 0 until nMels) {

                var energy = 0.0

                for (k in spectrum.indices) {
                    energy +=
                        spectrum[k] *
                                melBank[mel][k]
                }

                val logEnergy =
                    10.0 *
                            log10(
                                maxOf(
                                    energy,
                                    1e-10
                                )
                            )

                output[
                    frame * nMels + mel
                ] = logEnergy.toFloat()
            }
        }

        normalizeFeatures(
            output,
            frameCount,
            nMels
        )

        return output
    }

    private fun createMelFilters(
        sampleRate: Int,
        nFft: Int,
        nMels: Int
    ): Array<FloatArray> {

        val filters =
            Array(nMels) {
                FloatArray(nFft / 2 + 1)
            }

        val minMel =
            hzToMel(0.0)

        val maxMel =
            hzToMel(
                sampleRate / 2.0
            )

        val melPoints =
            DoubleArray(nMels + 2)

        for (i in melPoints.indices) {
            melPoints[i] =
                minMel +
                        (maxMel - minMel) *
                        i /
                        (nMels + 1)
        }

        val hzPoints =
            DoubleArray(nMels + 2)

        for (i in hzPoints.indices) {
            hzPoints[i] =
                melToHz(melPoints[i])
        }

        val bin =
            IntArray(nMels + 2)

        for (i in bin.indices) {
            bin[i] =
                (
                        hzPoints[i] *
                                nFft /
                                sampleRate
                        ).toInt()
        }

        for (m in 1..nMels) {

            val left =
                bin[m - 1]

            val center =
                bin[m]

            val right =
                bin[m + 1]

            for (k in left until center) {
                if (
                    k >= 0 &&
                    k < filters[m - 1].size &&
                    center > left
                ) {
                    filters[m - 1][k] =
                        (
                                k - left
                                ).toFloat() /
                                (center - left)
                }
            }

            for (k in center until right) {
                if (
                    k >= 0 &&
                    k < filters[m - 1].size &&
                    right > center
                ) {
                    filters[m - 1][k] =
                        (
                                right - k
                                ).toFloat() /
                                (right - center)
                }
            }
        }

        return filters
    }

    private fun normalizeFeatures(
        features: FloatArray,
        frameCount: Int,
        featureCount: Int
    ) {

        for (feature in 0 until featureCount) {

            var mean = 0.0

            for (frame in 0 until frameCount) {
                mean +=
                    features[
                        frame * featureCount +
                                feature
                    ]
            }

            mean /= frameCount

            for (frame in 0 until frameCount) {
                val index =
                    frame * featureCount +
                            feature

                features[index] =
                    (
                            features[index] -
                                    mean
                            ).toFloat()
            }
        }
    }

    private fun hzToMel(
        hz: Double
    ): Double {
        return 2595.0 *
                log10(
                    1.0 + hz / 700.0
                )
    }

    private fun melToHz(
        mel: Double
    ): Double {
        return 700.0 *
                (
                        Math.pow(
                            10.0,
                            mel / 2595.0
                        ) - 1.0
                        )
    }

    fun calculateCosineSimilarity(
        vectorA: FloatArray,
        vectorB: FloatArray
    ): Float {

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vectorA.indices) {
            dotProduct +=
                vectorA[i] * vectorB[i]

            normA +=
                vectorA[i] * vectorA[i]

            normB +=
                vectorB[i] * vectorB[i]
        }

        if (
            normA == 0.0f ||
            normB == 0.0f
        ) {
            return 0.0f
        }

        return dotProduct /
                (
                        sqrt(normA) *
                                sqrt(normB)
                        )
    }

    fun verifyOwner(
        audioSamples: ShortArray,
        ownerEmbedding: FloatArray,
        threshold: Float = 0.65f
    ): Boolean {

        val currentEmbedding =
            extractEmbedding(audioSamples)

        val similarity =
            calculateCosineSimilarity(
                currentEmbedding,
                ownerEmbedding
            )

        return similarity >= threshold
    }

    fun close() {
        session.close()
        env.close()
    }
}

