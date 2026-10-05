package com.example.yupi.audio

class RollingAudioBuffer(
    private val bufferSizeInSamples: Int
) {

    private val buffer =
        ShortArray(bufferSizeInSamples)

    private var writeIndex = 0
    private var isFull = false

    @Synchronized
    fun write(
        data: ShortArray,
        length: Int
    ) {
        for (i in 0 until length) {
            buffer[writeIndex] = data[i]

            writeIndex =
                (writeIndex + 1) % bufferSizeInSamples

            if (writeIndex == 0) {
                isFull = true
            }
        }
    }

    @Synchronized
    fun getSnapshot(): ShortArray {
        val size =
            if (isFull) {
                bufferSizeInSamples
            } else {
                writeIndex
            }

        val result = ShortArray(size)

        if (!isFull) {
            System.arraycopy(
                buffer,
                0,
                result,
                0,
                writeIndex
            )
        } else {
            val headSize =
                bufferSizeInSamples - writeIndex

            System.arraycopy(
                buffer,
                writeIndex,
                result,
                0,
                headSize
            )

            System.arraycopy(
                buffer,
                0,
                result,
                headSize,
                writeIndex
            )
        }

        return result
    }

    @Synchronized
    fun clear() {
        writeIndex = 0
        isFull = false
    }
}