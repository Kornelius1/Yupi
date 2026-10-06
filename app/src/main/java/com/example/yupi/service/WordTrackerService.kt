package com.example.yupi.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.yupi.audio.SileroVadDetector
import com.example.yupi.audio.SpeakerVerifier
import com.example.yupi.audio.SyllableDetector
import com.example.yupi.audio.VoiceProfileManager
import com.example.yupi.data.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.yupi.analytics.SocialPatternAnalyzer
import com.example.yupi.analytics.model.SocialMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WordTrackerService : Service() {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @Volatile private var isRecording = false
    @Volatile private var isInitialized = false

    private var recordingJob: Job? = null
    private var processingJob: Job? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val utteranceChannel = Channel<ShortArray>(capacity = 3)

    private var vadDetector: SileroVadDetector? = null
    private var verifier: SpeakerVerifier? = null
    private var profileManager: VoiceProfileManager? = null
    private var syllableDetector: SyllableDetector? = null
    private var repository: WordRepository? = null
    private var ownerEmbedding: FloatArray? = null

    private var socialPatternAnalyzer: SocialPatternAnalyzer? = null


    override fun onCreate() {
        super.onCreate()
        running = true
        Log.d(TAG, "onCreate: Menjalankan Foreground Service")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch(Dispatchers.IO) { initHeavyComponents() }
    }

    private fun initHeavyComponents() {
        try {
            Log.d(TAG, "initHeavyComponents: Memuat model ML di Background Thread")
            vadDetector = SileroVadDetector(this@WordTrackerService)
            verifier = SpeakerVerifier(this@WordTrackerService)
            profileManager = VoiceProfileManager(this@WordTrackerService)
            syllableDetector = SyllableDetector(sampleRate)
            repository = WordRepository(this@WordTrackerService)
            ownerEmbedding = profileManager?.getOwnerEmbedding()
            socialPatternAnalyzer = SocialPatternAnalyzer(similarityThreshold = OWNER_THRESHOLD)
            isInitialized = true
            Log.d(TAG, "initHeavyComponents: Inisialisasi komponen selesai")
        } catch (e: Exception) {
            Log.e(TAG, "initHeavyComponents: Gagal memuat komponen ML", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!VoiceProfileManager(this).isProfileRegistered()) {
            Log.w(TAG, "Profil suara belum ada, service dihentikan")
            stopSelf()
            return START_NOT_STICKY
        }
        Log.d(TAG, "onStartCommand: Memulai perintah startRecording")
        serviceScope.launch(Dispatchers.IO) {
            while (!isInitialized) delay(100)
            startRecording()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (isRecording) return

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf <= 0) {
            Log.e(TAG, "minBufferSize error: $minBuf"); stopSelf(); return
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBuf * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            Log.e(TAG, "AudioRecord gagal init"); stopSelf(); return
        }

        record.startRecording()
        isRecording = true
        Log.d(TAG, "startRecording: AudioRecord berhasil di-start")

        processingJob = serviceScope.launch(Dispatchers.Default) { processUtterances() }

        // Coroutine ini pemilik AudioRecord dan VAD, jadi release di sini aman dari race
        recordingJob = serviceScope.launch(Dispatchers.IO) {
            try {
                readLoop(record)
            } finally {
                runCatching { record.stop() }
                record.release()
                vadDetector?.close()
            }
        }
    }

    private suspend fun readLoop(record: AudioRecord) {
        val vad = vadDetector ?: return
        val chunk = ShortArray(CHUNK)
        val utterance = ShortArray(MAX_UTTERANCE + (END_SILENCE_CHUNKS + 1) * CHUNK)
        var length = 0
        var silentChunks = 0
        var loop = 0L
        var maxProb = 0f

        fun flush() {
            if (length >= MIN_UTTERANCE) {
                if (!utteranceChannel.trySend(utterance.copyOf(length)).isSuccess) {
                    Log.w(TAG, "Antrean penuh, ucapan dibuang")
                }
            }
            length = 0
            silentChunks = 0
        }

        while (isRecording && currentCoroutineContext().isActive) {
            val n = record.read(chunk, 0, CHUNK)
            if (n < 0) { Log.e(TAG, "read error: $n"); stopSelf(); return }
            if (n != CHUNK) continue

            val prob = vad.getSpeechProbability(chunk)
            val speech = prob >= 0.5f
            if (prob > maxProb) maxProb = prob
            if (++loop % 100 == 0L) {
                Log.d(TAG, "vad max=" + "%.2f".format(maxProb))
                maxProb = 0f
            }

            if (speech) {
                System.arraycopy(chunk, 0, utterance, length, CHUNK)
                length += CHUNK
                silentChunks = 0
                if (length >= MAX_UTTERANCE) flush()
            } else if (length > 0) {
                System.arraycopy(chunk, 0, utterance, length, CHUNK)
                length += CHUNK
                if (++silentChunks >= END_SILENCE_CHUNKS) flush()
            }
        }
    }

    private suspend fun processUtterances() {
        val v = verifier ?: return
        val syl = syllableDetector ?: return
        val repo = repository ?: return
        val analyzer = socialPatternAnalyzer ?: return
        val owner = ownerEmbedding
        if (owner == null) {
            Log.e(TAG, "Profil suara belum ada"); stopSelf(); return
        }

        try {
            for (audio in utteranceChannel) {
                val t0 = SystemClock.elapsedRealtime()
                val forVerify = if (audio.size > VERIFY_MAX) audio.copyOf(VERIFY_MAX) else audio
                val similarity = v.calculateCosineSimilarity(v.extractEmbedding(forVerify), owner)
                val isOwner = similarity >= OWNER_THRESHOLD
                val took = SystemClock.elapsedRealtime() - t0
                val durationMs = (audio.size / 16).toLong()

                val syllables = syl.countSyllablesInBuffer(audio)
                val words = syl.estimateWordCount(syllables)
                analyzer.processUtterance(
                    similarity = similarity,
                    durationMs = durationMs,
                    wordCount = if (isOwner) words else 0
                )

                Log.d(
                    TAG,
                    "ucapan $durationMs ms, similarity=" + "%.2f".format(similarity) +
                            ", pemilik=$isOwner, verifikasi=$took ms"
                )
                _socialMetrics.value = analyzer.metrics.value
                if (!isOwner) continue

                Log.d(TAG, "suku kata=$syllables, kata=$words")
                if (words > 0) repo.incrementTodayWordCount(words)
            }
        } finally {
            v.close()
        }
    }

    override fun onDestroy() {
        running = false
        Log.d(TAG, "onDestroy: Service dihentikan")
        isRecording = false
        serviceScope.cancel() // blok finally di atas yang melepas AudioRecord dan model
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Yupi Word Tracker", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yupi Aktif")
            .setContentText("Aplikasi sedang memantau ucapan harian.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "WordTrackerService"
        private const val CHANNEL_ID = "yupi_tracker_channel"
        private const val NOTIFICATION_ID = 1001

        private const val CHUNK = 512
        private const val END_SILENCE_CHUNKS = 20      // ±0,64 detik hening = ucapan selesai
        private const val MIN_UTTERANCE = 24000        // minimal 1,5 detik
        private const val MAX_UTTERANCE = 16000 * 10   // potong di 10 detik
        private const val VERIFY_MAX = 48000           // 3 detik, sama dengan pendaftaran
        private const val OWNER_THRESHOLD = 0.41f

        private val _socialMetrics = MutableStateFlow(SocialMetrics())
        val socialMetrics: StateFlow<SocialMetrics> = _socialMetrics.asStateFlow()
        @Volatile var running = false
    }
}