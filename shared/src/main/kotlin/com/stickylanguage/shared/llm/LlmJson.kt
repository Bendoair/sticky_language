package com.stickylanguage.shared.llm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Shared JSON config for parsing structured LLM responses (cloud + local). */
object LlmJson {
    val instance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun <T> decode(serializer: KSerializer<T>, text: String): T =
        instance.decodeFromString(serializer, stripFences(text))

    fun stripFences(text: String): String {
        val t = text.trim()
        if (!t.startsWith("```")) return t
        return t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }
}
