package com.example.yupi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.yupi.audio.VoiceProfileManager
import com.example.yupi.data.WordRepository
import com.example.yupi.service.WordTrackerService
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {

    private val permissionRequestCode = 101

    private lateinit var profileManager: VoiceProfileManager
    private lateinit var chart: PieChart
    private var chartInitialized = false
    private var lastOwnerSec = -1L
    private var lastOtherSec = -1L
    private var shownOn: Boolean? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<android.view.View>(R.id.rootContent)
        val extraTop = (4 * resources.displayMetrics.density).toInt()
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top + extraTop, bottom = baseBottom + bars.bottom)
            insets
        }

        profileManager = VoiceProfileManager(this)
        checkAndRequestPermissions()

        val btnRegisterVoice = findViewById<Button>(R.id.btnRegisterVoice)
        val power = findViewById<LottieAnimationView>(R.id.powerButton)
        val tvPowerLabel = findViewById<TextView>(R.id.tvPowerLabel)
        chart = findViewById(R.id.chartTalk)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvTodayWordCount = findViewById<TextView>(R.id.tvTodayWordCount)
        val tvOwnerDuration = findViewById<TextView>(R.id.tvOwnerDuration)
        val tvInterlocutorDuration = findViewById<TextView>(R.id.tvInterlocutorDuration)
        val tvTurnCount = findViewById<TextView>(R.id.tvTurnCount)
        val tvDialogueVsMonologue = findViewById<TextView>(R.id.tvDialogueVsMonologue)
        val tvInteractivityScore = findViewById<TextView>(R.id.tvInteractivityScore)

        val repo = WordRepository(this)

        btnRegisterVoice.setOnClickListener {
            startActivity(Intent(this, VoiceRegistrationActivity::class.java))
        }

        // Efek zoom + animasi stop saat disentuh. Mengembalikan false supaya klik tetap diteruskan.
        power.setOnTouchListener { v, e ->
            val lottie = v as LottieAnimationView
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).start()
                    if (shownOn != true) {
                        lottie.repeatCount = 0
                        lottie.playAnimation()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300)
                        .setInterpolator(OvershootInterpolator(2.5f)).start()
                    if (e.action == MotionEvent.ACTION_CANCEL && shownOn != true) {
                        lottie.cancelAnimation()
                        lottie.progress = 0f
                    }
                }
            }
            false
        }

        power.setOnClickListener {
            val serviceIntent = Intent(this, WordTrackerService::class.java)
            if (WordTrackerService.running) {
                stopService(serviceIntent)
                power.setActive(false)
                tvPowerLabel.text = "Ketuk untuk mulai"
            } else {
                if (!profileManager.isProfileRegistered()) {
                    Toast.makeText(this, "Daftarkan suara pemilik terlebih dahulu!", Toast.LENGTH_LONG).show()
                    power.resetStop()
                    return@setOnClickListener
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Izin mikrofon dibutuhkan", Toast.LENGTH_LONG).show()
                    checkAndRequestPermissions()
                    power.resetStop()
                    return@setOnClickListener
                }
                startForegroundService(serviceIntent)
                power.setActive(true)
                tvPowerLabel.text = "Tracking aktif, ketuk untuk berhenti"
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    tvStatus.text =
                        if (profileManager.isProfileRegistered()) "Status: Suara Pemilik Terdaftar"
                        else "Status: Belum Melakukan Registrasi Suara"

                    // Sinkron dengan kondisi service (mis. service berhenti sendiri)
                    val running = WordTrackerService.running
                    power.setActive(running)
                    tvPowerLabel.text =
                        if (running) "Tracking aktif, ketuk untuk berhenti" else "Ketuk untuk mulai"

                    tvTodayWordCount.text = "Total Kata Hari Ini: ${repo.getTodayWordCount()} kata"

                    val metrics = WordTrackerService.socialMetrics.value
                    tvOwnerDuration.text = "Durasi Bicara Subjek: ${metrics.ownerDurationMs.toFormattedTime()}"
                    tvInterlocutorDuration.text = "Durasi Lawan Bicara: ${metrics.interlocutorDurationMs.toFormattedTime()}"
                    tvTurnCount.text = "Pergantian Pembicara: ${metrics.turnCount} kali"
                    tvDialogueVsMonologue.text = "Durasi Dialog: ${metrics.dialogueDurationMs.toFormattedTime()} | Monolog: ${metrics.monologueDurationMs.toFormattedTime()}"
                    tvInteractivityScore.text = "Skor Interaktivitas: ${"%.1f".format(metrics.interactivityScore)} turn/menit"

                    updateTalkChart(metrics.ownerDurationMs / 1000, metrics.interlocutorDurationMs / 1000)

                    delay(1000)
                }
            }
        }
    }

    private fun LottieAnimationView.setActive(on: Boolean) {
        if (shownOn == on) return
        shownOn = on
        setAnimation(if (on) R.raw.play_icon else R.raw.stop_icon)
        if (on) {
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        } else {
            repeatCount = 0
            progress = 0f
        }
    }

    // Dipakai kalau klik ditolak (profil/izin belum ada), supaya ikon stop tidak berhenti di tengah animasi
    private fun LottieAnimationView.resetStop() {
        cancelAnimation()
        progress = 0f
    }

    private fun updateTalkChart(ownerSec: Long, otherSec: Long) {
        if (ownerSec == lastOwnerSec && otherSec == lastOtherSec) return
        lastOwnerSec = ownerSec
        lastOtherSec = otherSec

        val empty = ownerSec == 0L && otherSec == 0L
        val entries = if (empty) {
            listOf(PieEntry(1f, "Belum ada data"))
        } else {
            listOf(
                PieEntry(ownerSec.toFloat(), "Anda"),
                PieEntry(otherSec.toFloat(), "Lawan bicara")
            )
        }
        val set = PieDataSet(entries, "").apply {
            colors = if (empty) listOf(Color.parseColor("#DDDDDD"))
            else listOf(Color.parseColor("#303F9F"), Color.parseColor("#FFB74D"))
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            setDrawValues(!empty)
        }
        chart.apply {
            data = PieData(set)
            isDrawHoleEnabled = true
            holeRadius = 62f
            setHoleColor(Color.TRANSPARENT)
            description.isEnabled = false
            setDrawEntryLabels(false)
            legend.textColor = Color.parseColor("#5B6075")
            if (!chartInitialized) {
                chartInitialized = true
                animateY(600)
            }
            invalidate()
        }
    }

    private fun Long.toFormattedTime(): String {
        val totalSeconds = this / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                permissionRequestCode
            )
        }
    }
}