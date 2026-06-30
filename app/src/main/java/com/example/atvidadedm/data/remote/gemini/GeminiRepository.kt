package com.example.atvidadedm.data.remote.gemini

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import retrofit2.HttpException

class GeminiRepository(
    private val apiService: GeminiApiService,
    private val apiKey: String
) {
    private companion object {
        const val TAG = "GeminiRepository"
        const val MODEL = "gemini-2.5-flash"
    }

    suspend fun generateItinerary(prompt: String): Result<String> {
        if (apiKey.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY nao configurada. Nao foi possivel gerar roteiro.")
            return Result.failure(
                IllegalStateException(
                    "Chave da API do Gemini não configurada. Adicione GEMINI_API_KEY em local.secrets.properties."
                )
            )
        }

        return runCatching {
            val sanitizedPrompt = prompt.trim()
            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiGenerateContent(
                        parts = listOf(GeminiGenerateContentPart(text = sanitizedPrompt))
                    )
                )
            )
            Log.d(
                TAG,
                "Iniciando chamada Gemini. promptLength=${sanitizedPrompt.length}, promptPreview=${sanitizedPrompt.take(140)}"
            )

            val response = executeGenerateContentWithRetry(request)

            Log.d(
                TAG,
                "Resposta Gemini recebida. output=${!response.output.isNullOrBlank()}, text=${!response.text.isNullOrBlank()}, response=${!response.response.isNullOrBlank()}, result=${!response.result.isNullOrBlank()}, generatedText=${!response.generatedText.isNullOrBlank()}, candidates=${response.candidates?.size ?: 0}, outputs=${response.outputs?.size ?: 0}"
            )

            val extracted = response.extractText()?.trim().orEmpty()
            if (extracted.isNotBlank()) {
                return@runCatching extracted
            }

            // Fallback: parse raw JSON for unexpected response shapes.
            val rawResponse = runCatching {
                withContext(Dispatchers.IO) {
                    apiService.generateContentFromModelRaw(
                        model = MODEL,
                        apiKey = apiKey,
                        request = request
                    )
                }
            }

            val fallbackText = rawResponse.getOrNull()?.extractTextFallback()?.trim().orEmpty()
            fallbackText.ifBlank {
                Log.e(TAG, "Gemini respondeu sem texto utilizavel apos extractText().")
                throw IllegalStateException("A Gemini não retornou texto para o roteiro.")
            }
        }.recoverCatching { throwable ->
            Log.e(TAG, "Falha ao gerar roteiro com Gemini: ${throwable.message}", throwable)
            when (throwable) {
                is SocketTimeoutException -> {
                    throw IllegalStateException(
                        "A IA demorou para responder. Tente novamente ou simplifique o roteiro."
                    )
                }
                is HttpException -> {
                    if (throwable.code() == 503) {
                        throw IllegalStateException(
                            "A IA está temporariamente indisponível (503). Aguarde alguns segundos e tente gerar o roteiro novamente."
                        )
                    }
                    throw IllegalStateException(
                        "Falha na IA (${throwable.code()}). Verifique a chave Gemini e tente novamente."
                    )
                }
                else -> throw throwable
            }
        }
    }

    private suspend fun executeGenerateContentWithRetry(
        request: GeminiGenerateContentRequest
    ): GeminiInteractionResponse {
        var lastError: Throwable? = null

        repeat(3) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    apiService.generateContentFromModel(
                        model = MODEL,
                        apiKey = apiKey,
                        request = request
                    )
                }
            } catch (throwable: Throwable) {
                lastError = throwable
                val shouldRetry = throwable is HttpException && throwable.code() == 503 && attempt < 2
                if (!shouldRetry) {
                    throw throwable
                }
                val delayMillis = 1_000L * (attempt + 1)
                Log.w(TAG, "Gemini 503 na tentativa ${attempt + 1}. Tentando novamente em ${delayMillis}ms.")
                delay(delayMillis)
            }
        }

        throw lastError ?: IllegalStateException("Falha inesperada ao chamar o Gemini.")
    }
}
