package com.example.yupi.audio

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

class SileroVadDetector(context: Context) {

    private val env: OrtEnvironment =
        OrtEnvironment.getEnvironment()

    private val session: OrtSession

    private var state =
        Array(2) {
            Array(1) {
                FloatArray(128)
            }
        }

    private var context = FloatArray(64)

    init {
        val modelBytes =
            context.assets
                .open("silero_vad.onnx")
                .readBytes()

        session =
            env.createSession(
                modelBytes,
                OrtSession.SessionOptions()
            )


    }


    fun getSpeechProbability(
        audioSamples: ShortArray
    ): Float {

        val floatAudio =
            FloatArray(audioSamples.size)

        for (i in audioSamples.indices) {
            floatAudio[i] =
                audioSamples[i] / 32768.0f
        }

        val input = FloatArray(64 + floatAudio.size)
        System.arraycopy(context, 0, input, 0, 64)
        System.arraycopy(floatAudio, 0, input, 64, floatAudio.size)
        context = floatAudio.copyOfRange(floatAudio.size - 64, floatAudio.size)

        val inputShape = longArrayOf(1, input.size.toLong())

        val inputTensor =
            OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                inputShape
            )

        val srTensor =
            OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(
                    longArrayOf(16000)
                ),
                longArrayOf()
            )

        val stateTensor =
            OnnxTensor.createTensor(
                env,
                state
            )

        val inputs =
            mapOf(
                "input" to inputTensor,
                "state" to stateTensor,
                "sr" to srTensor
            )

        val results =
            session.run(inputs)

        val outputTensor =
            results[0].value as Array<FloatArray>

        val probability =
            outputTensor[0][0]

        @Suppress("UNCHECKED_CAST")
        state =
            results[1].value
                    as Array<Array<FloatArray>>

        inputTensor.close()
        srTensor.close()
        stateTensor.close()
        results.close()

        return probability
    }

    fun close() {
        session.close()
        env.close()
    }
}