package com.example.yupi.audio

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class VoiceProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "voice_profile_prefs",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    fun saveOwnerEmbedding(
        embedding: FloatArray
    ) {
        val jsonString =
            gson.toJson(embedding)

        prefs.edit()
            .putString(
                "owner_embedding",
                jsonString
            )
            .apply()
    }

    fun getOwnerEmbedding(): FloatArray? {
        val jsonString =
            prefs.getString(
                "owner_embedding",
                null
            ) ?: return null

        return gson.fromJson(
            jsonString,
            FloatArray::class.java
        )
    }

    fun isProfileRegistered(): Boolean {
        return prefs.contains(
            "owner_embedding"
        )
    }
}