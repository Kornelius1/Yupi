package com.example.yupi

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yupi.audio.SpeakerVerifier
import com.example.yupi.audio.VoiceProfileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class VoiceRegistrationActivity : AppCompatActivity() {

    private lateinit var verifier: SpeakerVerifier
    private lateinit var profileManager: VoiceProfileManager
    private lateinit var tvStatus: TextView
    private lateinit var btnRecord: Button

    private val activityScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_registration)

        val root = findViewById<android.view.View>(R.id.rootRegistration)
        val extraTop = (16 * resources.displayMetrics.density).toInt()
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top + extraTop, bottom = baseBottom + bars.bottom)
            insets
        }

        verifier = SpeakerVerifier(this)
        profileManager = VoiceProfileManager(this)

        tvStatus =
            findViewById(R.id.tvRegistrationStatus)

        btnRecord =
            findViewById(R.id.btnRecordVoice)

        if (profileManager.isProfileRegistered()) {
            tvStatus.text = "Suara sudah terdaftar. Rekam ulang untuk menggantinya."
            btnRecord.text = "Rekam Ulang"
        }

        btnRecord.setOnClickListener {
            startVoiceEnrollment()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startVoiceEnrollment() {

        btnRecord.isEnabled = false

        tvStatus.text =
            "Silakan bicara selama 3 detik..."

        activityScope.launch {

            val sampleRate = 16000
            val durationInSeconds = 3
            val totalSamples =
                sampleRate * durationInSeconds

            val audioData =
                ShortArray(totalSamples)

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            if (minBufferSize <= 0) {
                withContext(Dispatchers.Main) {
                    tvStatus.text =
                        "Gagal menyiapkan perekaman suara."

                    btnRecord.isEnabled = true
                }

                return@launch
            }

            val recorder =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
                )

            if (
                recorder.state !=
                AudioRecord.STATE_INITIALIZED
            ) {
                recorder.release()

                withContext(Dispatchers.Main) {
                    tvStatus.text =
                        "Gagal menginisialisasi microphone."

                    btnRecord.isEnabled = true
                }

                return@launch
            }

            try {
                recorder.startRecording()

                var readSize = 0

                while (readSize < totalSamples) {

                    val read =
                        recorder.read(
                            audioData,
                            readSize,
                            totalSamples - readSize
                        )

                    if (read > 0) {
                        readSize += read
                    }
                }

                recorder.stop()

                val ownerEmbedding =
                    verifier.extractEmbedding(
                        audioData
                    )

                profileManager.saveOwnerEmbedding(
                    ownerEmbedding
                )

                withContext(Dispatchers.Main) {

                    tvStatus.text =
                        "Profil suara pemilik berhasil disimpan!"

                    btnRecord.isEnabled = true

                    Toast.makeText(
                        this@VoiceRegistrationActivity,
                        "Registrasi Selesai",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } finally {
                recorder.release()
            }
        }
    }

    override fun onDestroy() {

        activityScope.cancel()

        verifier.close()

        super.onDestroy()
    }
}