package com.example.atvidadedm.data.remote.gemini

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentFromModel(
        @Path(value = "model", encoded = true) model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiInteractionResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentFromModelRaw(
        @Path(value = "model", encoded = true) model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): JsonObject
}

