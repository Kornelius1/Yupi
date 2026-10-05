package com.example.yupi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.yupi.audio.VoiceProfileManager
import com.example.yupi.service.WordTrackerService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.TextView
import com.example.yupi.data.WordRepository

class MainActivity : AppCompatActivity() {

    private val permissionRequestCode = 101

    private lateinit var profileManager: VoiceProfileManager

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        profileManager =
            VoiceProfileManager(this)

        checkAndRequestPermissions()

        val btnRegisterVoice =
            findViewById<Button>(
                R.id.btnRegisterVoice
            )

        val btnStart =
            findViewById<Button>(
                R.id.btnStartService
            )

        val btnStop =
            findViewById<Button>(
                R.id.btnStopService
            )

        btnRegisterVoice.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    VoiceRegistrationActivity::class.java
                )
            )
        }

        btnStart.setOnClickListener {

            if (
                !profileManager
                    .isProfileRegistered()
            ) {

                Toast.makeText(
                    this,
                    "Daftarkan suara pemilik terlebih dahulu!",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val serviceIntent =
                Intent(
                    this,
                    WordTrackerService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(
                    serviceIntent
                )

            } else {

                startService(
                    serviceIntent
                )
            }

            Toast.makeText(
                this,
                "Tracking suara dimulai",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnStop.setOnClickListener {

            val serviceIntent =
                Intent(
                    this,
                    WordTrackerService::class.java
                )

            stopService(serviceIntent)

            Toast.makeText(
                this,
                "Tracking suara dihentikan",
                Toast.LENGTH_SHORT
            ).show()
        }

        val tvToday = findViewById<TextView>(R.id.tvToday)
        val repo = WordRepository(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    tvToday.text = "Kata hari ini: ${repo.getTodayWordCount()}"
                    delay(2000)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {

        val permissions =
            mutableListOf(
                Manifest.permission.RECORD_AUDIO
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        val missingPermissions =
            permissions.filter {

                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (
            missingPermissions.isNotEmpty()
        ) {

            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                permissionRequestCode
            )
        }
    }
}