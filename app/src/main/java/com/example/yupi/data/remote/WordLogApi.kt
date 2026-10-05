package com.example.yupi.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface WordLogApi {

    @POST("api/word-logs")
    suspend fun syncWordLog(
        @Body request: WordLogRequest
    ): Response<WordLogResponse>
}