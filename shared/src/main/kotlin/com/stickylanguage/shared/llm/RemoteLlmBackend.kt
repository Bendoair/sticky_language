package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceTranslation
import com.stickylanguage.shared.model.TranslationResult
import com.stickylanguage.shared.model.WordDetails
import com.stickylanguage.shared.model.WordQuery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import java.io.IOException

/**
 * Ktor-based client for an OpenAI-compatible chat completions endpoint that returns
 * strictly structured JSON. Config is resolved per call so Settings changes apply immediately.
 */
class RemoteLlmBackend(
    engine: HttpClientEngine,
    private val configProvider: suspend () -> LlmConfig,
) : LlmBackend {
    private val http = HttpClient(engine) {
        install(ContentNegotiation) { json(LlmJson.instance) }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 120_000
        }
        expectSuccess = false
    }

    override suspend fun translate(source: String, learned: Language, explain: Language): TranslationResult =
        SentenceChunker.translateEachSentence(source, learned, explain, CONTEXT_SENTENCES, CONCURRENT_REQUESTS) { sentence, previous ->
            complete(
                schemaName = "sticky_sentence_translation",
                schema = LlmSchemas.sentenceTranslation,
                system = LlmPrompts.translateSentenceSystem(learned, explain),
                user = LlmPrompts.translateSentenceUser(sentence, previous, learned, explain),
                serializer = SentenceTranslation.serializer(),
                maxTokens = (sentence.length * 6 + 1024).coerceIn(MIN_OUTPUT_TOKENS, MAX_OUTPUT_TOKENS),
            ).translation
        }

    override suspend fun breakdown(source: String, learned: Language, explain: Language): BreakdownResult =
        complete(
            schemaName = "sticky_breakdown",
            schema = LlmSchemas.breakdown,
            system = LlmPrompts.breakdownSystem(learned, explain),
            user = LlmPrompts.breakdownUser(source, learned, explain),
            serializer = BreakdownResult.serializer(),
        )

    override suspend fun lookupWords(queries: List<WordQuery>, learned: Language, explain: Language): List<WordDetails> =
        queries.chunked(LOOKUP_BATCH_SIZE).flatMap { batch ->
            val response = complete(
                schemaName = "sticky_word_details",
                schema = LlmSchemas.wordDetails,
                system = LlmPrompts.wordLookupSystem(learned, explain),
                user = LlmPrompts.wordLookupUser(batch, learned, explain),
                serializer = WordDetailsBatch.serializer(),
                maxTokens = (batch.size * 250 + 1024).coerceIn(MIN_OUTPUT_TOKENS, MAX_OUTPUT_TOKENS),
            )
            alignWordDetails(batch, response.words)
        }

    private suspend fun <T> complete(
        schemaName: String,
        schema: JsonObject,
        system: String,
        user: String,
        serializer: KSerializer<T>,
        maxTokens: Int? = null,
    ): T {
        val config = configProvider()
        if (!config.isConfigured) throw LlmException.NotConfigured()

        // Layer 1: provider-enforced JSON schema. Fall back to plain JSON mode + schema in the prompt
        // if the endpoint rejects `json_schema`.
        var useSchema = true
        var invalidRetryLeft = 1
        var lastError: LlmException? = null

        while (true) {
            val messages = if (useSchema) {
                listOf(ChatMessage("system", system), ChatMessage("user", user))
            } else {
                listOf(
                    ChatMessage("system", system + LlmPrompts.schemaFallbackSuffix(LlmSchemas.describe(schema))),
                    ChatMessage("user", user),
                )
            }
            val request = ChatCompletionRequest(
                model = config.model,
                messages = messages,
                maxTokens = maxTokens,
                responseFormat = if (useSchema) ResponseFormat.schema(schemaName, schema) else ResponseFormat.JsonObject,
            )

            val response: HttpResponse = try {
                http.post(config.chatCompletionsUrl) {
                    bearerAuth(config.apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                throw LlmException.Network(e)
            } catch (e: Exception) {
                throw LlmException.Network(e)
            }

            val status = response.status.value
            if (status !in 200..299) {
                val body = response.bodyAsText()
                if (useSchema && status == 400 && looksLikeSchemaUnsupported(body)) {
                    useSchema = false
                    continue
                }
                throw LlmException.Http(status, body)
            }

            val choice = try {
                response.body<ChatCompletionResponse>().choices.firstOrNull()
            } catch (e: Exception) {
                throw LlmException.InvalidResponse("could not read completion envelope", e)
            }
            val content = choice?.message
            if (content?.refusal != null) throw LlmException.InvalidResponse("refusal: ${content.refusal}")
            // Repeating an identical request would only be cut off again.
            if (choice?.finishReason == "length") throw LlmException.InvalidResponse("the output was cut off at its length limit")
            val text = content?.content ?: throw LlmException.InvalidResponse("empty content")

            // Layer 3: strict client-side parsing with one retry.
            try {
                return LlmJson.decode(serializer, text)
            } catch (e: Exception) {
                lastError = LlmException.InvalidResponse("invalid JSON (${e.message?.take(120)})", e)
                if (invalidRetryLeft-- > 0) continue
                throw lastError
            }
        }
    }

    private fun looksLikeSchemaUnsupported(body: String): Boolean {
        val b = body.lowercase()
        return "response_format" in b || "json_schema" in b || "structured" in b || "schema" in b
    }

    private companion object {
        const val MIN_OUTPUT_TOKENS = 2048
        const val MAX_OUTPUT_TOKENS = 8192
        const val LOOKUP_BATCH_SIZE = 20
        const val CONTEXT_SENTENCES = 2
        const val CONCURRENT_REQUESTS = 3
    }
}

/** @deprecated Prefer [RemoteLlmBackend] / [LlmBackend]. Kept as a type alias for call-site clarity. */
typealias LlmClient = LlmBackend
