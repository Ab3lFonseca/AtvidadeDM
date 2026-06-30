package com.example.atvidadedm.data.remote.gemini

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


data class GeminiGenerateContentRequest(
    val contents: List<GeminiGenerateContent>
)

data class GeminiGenerateContent(
    val parts: List<GeminiGenerateContentPart>
)

data class GeminiGenerateContentPart(
    val text: String
)

data class GeminiInteractionResponse(
    val output: String? = null,
    val response: String? = null,
    val text: String? = null,
    val result: String? = null,
    val candidates: List<GeminiCandidate>? = null,
    val outputs: List<GeminiOutput>? = null,
    @SerializedName("generated_text")
    val generatedText: String? = null
)

data class GeminiOutput(
    val text: String? = null,
    val output: String? = null,
    val content: String? = null
)

data class GeminiCandidate(
    val content: GeminiContentResponse? = null,
    val output: String? = null,
    val text: String? = null
)

data class GeminiContentResponse(
    val parts: List<GeminiResponsePart> = emptyList(),
    val text: String? = null
)

data class GeminiResponsePart(
    val text: String? = null
)

fun GeminiInteractionResponse.extractText(): String? {
    return sequenceOf(
        output,
        response,
        text,
        result,
        generatedText,
        candidates?.firstOrNull()?.text,
        candidates?.firstOrNull()?.output,
        candidates?.firstOrNull()?.content?.text,
        candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text,
        outputs?.firstOrNull()?.text,
        outputs?.firstOrNull()?.output,
        outputs?.firstOrNull()?.content
    ).firstOrNull { !it.isNullOrBlank() }
}

fun JsonObject.extractTextFallback(): String? {
    fun JsonElement?.stringValue(): String? {
        if (this == null || this.isJsonNull) return null
        return if (this.isJsonPrimitive) this.asString else null
    }

    return sequenceOf(
        get("output").stringValue(),
        get("response").stringValue(),
        get("text").stringValue(),
        get("result").stringValue(),
        get("generated_text").stringValue(),
        getAsJsonArray("outputs")?.firstOrNull()?.asJsonObject?.run {
            sequenceOf(
                get("text").stringValue(),
                get("output").stringValue(),
                get("content").stringValue()
            ).firstOrNull { !it.isNullOrBlank() }
        },
        getAsJsonArray("candidates")?.firstOrNull()?.asJsonObject?.run {
            sequenceOf(
                get("text").stringValue(),
                get("output").stringValue(),
                getAsJsonObject("content")?.get("text").stringValue(),
                getAsJsonObject("content")?.getAsJsonArray("parts")?.firstOrNull()?.asJsonObject?.get("text").stringValue()
            ).firstOrNull { !it.isNullOrBlank() }
        }
    ).firstOrNull { !it.isNullOrBlank() }
}

