package com.stickylanguage.shared.llm

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * JSON Schemas sent as OpenAI "Structured Outputs" (`response_format.json_schema`, strict mode).
 * Strict mode requires every property to be listed in `required` and `additionalProperties: false`.
 * These must stay in sync with the `@Serializable` classes in `model/Translation.kt`.
 */
object LlmSchemas {

    private fun str(description: String) = buildJsonObject {
        put("type", "string")
        put("description", description)
    }

    private fun arr(items: JsonObject, description: String) = buildJsonObject {
        put("type", "array")
        put("description", description)
        put("items", items)
    }

    private fun obj(vararg props: Pair<String, JsonObject>) = buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(props.toMap()))
        put("required", JsonArray(props.map { JsonPrimitive(it.first) }))
        put("additionalProperties", false)
    }

    private fun stringArray(description: String) = arr(buildJsonObject { put("type", "string") }, description)

    private val wordDetail: JsonObject = obj(
        "surface" to str("The word exactly as listed in the request."),
        "lemma" to str("Dictionary / base form of the word."),
        "reading" to str("Romanization or reading. Empty string if not applicable."),
        "pos" to str("Part of speech, plus attached particle/ending if any, e.g. 'noun + topic particle'."),
        "meanings" to stringArray("One to three concise meanings in the explanation language."),
        "grammar" to str("One or two sentences explaining the grammar attached to this token (particle, conjugation, honorific). Empty if none."),
        "example" to str("A short example sentence in the learned language followed by ' — ' and its translation. Empty string if none."),
    )

    val wordDetails: JsonObject = obj(
        "words" to arr(wordDetail, "One entry per requested word, in the same order."),
    )

    val sentenceTranslation: JsonObject = obj(
        "translation" to str("Natural, context-aware translation of the text."),
    )

    val chunk: JsonObject = obj(
        "text" to str("Surface text of the chunk, exactly as in the sentence."),
        "role" to str("Syntactic role: Topic, Subject, Object, Time, Place, Destination, Companion, Predicate, Modifier, Connector..."),
        "morph" to str("Morphological composition, e.g. '도서관 (library) + 에 (to)'."),
        "explanation" to str("One or two sentences explaining this chunk in the explanation language."),
        "lemma" to str("Dictionary form of the head word of the chunk."),
        "reading" to str("Romanization or reading of the head word. Empty string if not applicable."),
        "meaning" to str("Concise meaning of the head word."),
    )

    val grammarPoint: JsonObject = obj(
        "name" to str("Short name of the grammar pattern, e.g. 'N은/는 — topic particle'."),
        "body" to str("Two to four sentences explaining usage, with one short example."),
    )

    val sentenceBreakdown: JsonObject = obj(
        "text" to str("The sentence exactly as in the source text."),
        "translation" to str("Natural translation of the sentence."),
        "chunks" to arr(chunk, "The sentence split into syntactic chunks in order; concatenated they cover the sentence."),
        "grammar_points" to arr(grammarPoint, "The one to four most important grammar patterns in this sentence."),
    )

    val breakdown: JsonObject = obj(
        "source_language" to str("ISO 639-1 code of the source language."),
        "explanation_language" to str("ISO 639-1 code of the explanation language."),
        "sentences" to arr(sentenceBreakdown, "Every sentence of the source text, in order."),
    )

    /** Human-readable shape description for providers without JSON-schema support. */
    fun describe(schema: JsonObject): String = schema.toString()

    internal fun emptyArray(): JsonArray = buildJsonArray { }
}
