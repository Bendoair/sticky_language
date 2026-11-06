package com.stickylanguage.shared.llm

data class LlmConfig(
    val apiKey: String,
    /** Base URL including the version segment, e.g. `https://api.openai.com/v1`. */
    val baseUrl: String,
    val model: String,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    val chatCompletionsUrl: String get() = baseUrl.trimEnd('/') + "/chat/completions"
}

sealed class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotConfigured : LlmException("No API key configured. Add one in Settings.")
    class LocalNotReady(detail: String) : LlmException(detail)
    class Http(val status: Int, body: String) : LlmException("LLM request failed ($status): ${body.take(300)}")
    class Network(cause: Throwable) : LlmException("Network error: ${cause.message ?: cause::class.simpleName}", cause)
    class InvalidResponse(detail: String, cause: Throwable? = null) :
        LlmException("The model returned an unexpected response: $detail", cause)
}
