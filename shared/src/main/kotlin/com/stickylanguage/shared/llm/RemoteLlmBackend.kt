package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.TranslationResult
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
        complete(
            schemaName = "sticky_translation",
            schema = LlmSchemas.translation,
            system = LlmPrompts.translateSystem(learned, explain),
            user = LlmPrompts.translateUser(source, learned, explain),
            serializer = TranslationResult.serializer(),
        ).let { result ->
            // Guarantee the source text is what the UI already shows.
            result.copy(sourceText = source)
        }

    override suspend fun breakdown(source: String, learned: Language, explain: Language): BreakdownResult =
        complete(
            schemaName = "sticky_breakdown",
            schema = LlmSchemas.breakdown,
            system = LlmPrompts.breakdownSystem(learned, explain),
            user = LlmPrompts.breakdownUser(source, learned, explain),
            serializer = BreakdownResult.serializer(),
        )

    private suspend fun <T> complete(
        schemaName: String,
        schema: JsonObject,
        system: String,
        user: String,
        serializer: KSerializer<T>,
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

            val content = try {
                response.body<ChatCompletionResponse>().choices.firstOrNull()?.message
            } catch (e: Exception) {
                throw LlmException.InvalidResponse("could not read completion envelope", e)
            }
            if (content?.refusal != null) throw LlmException.InvalidResponse("refusal: ${content.refusal}")
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
}

/** @deprecated Prefer [RemoteLlmBackend] / [LlmBackend]. Kept as a type alias for call-site clarity. */
typealias LlmClient = LlmBackend
