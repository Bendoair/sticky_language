package com.stickylanguage.shared.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
)

@Serializable
internal data class ResponseFormat(
    val type: String,
    @SerialName("json_schema") val jsonSchema: JsonSchemaFormat? = null,
) {
    companion object {
        val JsonObject = ResponseFormat(type = "json_object")
        fun schema(name: String, schema: JsonObject) = ResponseFormat(
            type = "json_schema",
            jsonSchema = JsonSchemaFormat(name = name, strict = true, schema = schema),
        )
    }
}

@Serializable
internal data class JsonSchemaFormat(
    val name: String,
    val strict: Boolean,
    val schema: JsonObject,
)

@Serializable
internal data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val message: ResponseMessage,
        @SerialName("finish_reason") val finishReason: String? = null,
    )

    @Serializable
    data class ResponseMessage(
        val role: String = "assistant",
        val content: String? = null,
        val refusal: String? = null,
    )
}
