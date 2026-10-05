package com.example.yupi.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.yupi.data.local.AppDatabase
import com.example.yupi.data.local.WordLogEntity
import com.example.yupi.data.remote.RetrofitClient
import com.example.yupi.data.remote.WordLogRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WordRepository(context: Context) {

    private val dao =
        AppDatabase.getDatabase(context)
            .wordLogDao()

    private val api =
        RetrofitClient.instance

    private val deviceId: String by lazy {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    fun getTodayDateString(): String {

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        return dateFormat.format(Date())
    }

    suspend fun getTodayWordCount(): Int =
        dao.getLogByDate(getTodayDateString())?.wordCount ?: 0

    suspend fun incrementTodayWordCount(
        addedWords: Int
    ): Int {

        val today =
            getTodayDateString()

        val existingLog =
            dao.getLogByDate(today)

        val currentCount =
            existingLog?.wordCount ?: 0

        val newCount =
            currentCount + addedWords

        val updatedEntity =
            WordLogEntity(
                date = today,
                wordCount = newCount,
                isSynced = false
            )

        dao.insertOrUpdate(updatedEntity)

        return newCount
    }

    suspend fun syncTodayDataToServer(): Boolean {

        val today =
            getTodayDateString()

        val log =
            dao.getLogByDate(today)
                ?: return false

        try {

            val response =
                api.syncWordLog(
                    WordLogRequest(
                        deviceId = deviceId,
                        date = log.date,
                        wordCount = log.wordCount
                    )
                )

            if (response.isSuccessful) {

                dao.updateSyncStatus(
                    today,
                    true
                )

                Log.d(
                    "SYNC_LOG",
                    "Berhasil sinkronisasi ke server: " +
                            "${log.wordCount} kata"
                )

                return true

            } else {

                Log.e(
                    "SYNC_LOG",
                    "Gagal sinkronisasi. " +
                            "Kode status: ${response.code()}"
                )
            }

        } catch (e: Exception) {

            Log.e(
                "SYNC_LOG",
                "Kesalahan jaringan saat " +
                        "sinkronisasi: ${e.message}"
            )
        }

        return false
    }
}