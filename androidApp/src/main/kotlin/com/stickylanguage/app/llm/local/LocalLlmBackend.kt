package com.stickylanguage.app.llm.local

import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.shared.llm.LlmBackend
import com.stickylanguage.shared.llm.LlmException
import com.stickylanguage.shared.llm.LlmJson
import com.stickylanguage.shared.llm.LlmPrompts
import com.stickylanguage.shared.llm.LlmSchemas
import com.stickylanguage.shared.llm.SentenceChunker
import com.stickylanguage.shared.llm.WordDetailsBatch
import com.stickylanguage.shared.llm.alignWordDetails
import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceTranslation
import com.stickylanguage.shared.model.TranslationResult
import com.stickylanguage.shared.model.WordDetails
import com.stickylanguage.shared.model.WordQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

enum class LocalEnginePhase {
    Idle,
    LoadingModel,
    Generating,
}

@Singleton
class LocalLlmBackend @Inject constructor(
    private val settings: SettingsRepository,
    private val store: ModelStore,
    private val engine: LlamaCppEngine,
) : LlmBackend {

    private val _phase = MutableStateFlow(LocalEnginePhase.Idle)
    val phase: StateFlow<LocalEnginePhase> = _phase.asStateFlow()

    override suspend fun translate(source: String, learned: Language, explain: Language): TranslationResult {
        ensureReady()
        // The engine runs one generation at a time; less context means less prompt to read on the CPU.
        return SentenceChunker.translateEachSentence(source, learned, explain, contextSentences = 1, concurrency = 1) { sentence, previous ->
            complete(
                system = LlmPrompts.translateSentenceSystem(learned, explain),
                user = LlmPrompts.translateSentenceUser(sentence, previous, learned, explain),
                schema = LlmSchemas.sentenceTranslation,
                serializer = SentenceTranslation.serializer(),
            ).translation
        }
    }

    override suspend fun breakdown(source: String, learned: Language, explain: Language): BreakdownResult {
        ensureReady()
        val chunks = if (SentenceChunker.shouldChunk(source)) SentenceChunker.split(source) else listOf(source.trim())
        val parts = chunks.map { chunk ->
            complete(
                system = LlmPrompts.breakdownSystem(learned, explain),
                user = LlmPrompts.breakdownUser(chunk, learned, explain),
                schema = LlmSchemas.breakdown,
                serializer = BreakdownResult.serializer(),
            )
        }
        return if (parts.size == 1) parts.first() else SentenceChunker.mergeBreakdowns(learned, explain, parts)
    }

    /** One word per call: small models can't reliably emit a large structured batch. */
    override suspend fun lookupWords(queries: List<WordQuery>, learned: Language, explain: Language): List<WordDetails> {
        ensureReady()
        return queries.map { query ->
            val single = listOf(query)
            val response = complete(
                system = LlmPrompts.wordLookupSystem(learned, explain),
                user = LlmPrompts.wordLookupUser(single, learned, explain),
                schema = LlmSchemas.wordDetails,
                serializer = WordDetailsBatch.serializer(),
            )
            alignWordDetails(single, response.words).single()
        }
    }

    private suspend fun ensureReady() {
        val s = settings.current()
        val spec = ModelCatalog.byId(s.localModelId)
            ?: throw LlmException.LocalNotReady("Unknown on-device model. Pick one in Settings.")
        if (!store.isReady(spec)) {
            throw LlmException.LocalNotReady("Download ${spec.displayName} in Settings before using on-device mode.")
        }
        if (engine.loadedModelId != spec.id) {
            _phase.value = LocalEnginePhase.LoadingModel
            try {
                engine.ensureLoaded(spec.id, store.fileFor(spec))
            } catch (e: Exception) {
                _phase.value = LocalEnginePhase.Idle
                throw LlmException.LocalNotReady("Failed to load ${spec.displayName}: ${e.message}")
            }
        }
    }

    private suspend fun <T> complete(
        system: String,
        user: String,
        schema: JsonObject,
        serializer: KSerializer<T>,
    ): T {
        val systemWithSchema = system + LlmPrompts.schemaFallbackSuffix(LlmSchemas.describe(schema))
        var retries = 1
        var lastError: LlmException? = null
        _phase.value = LocalEnginePhase.Generating
        try {
            while (true) {
                val text = try {
                    engine.complete(
                        system = systemWithSchema,
                        user = user,
                        jsonSchema = schema.toString(),
                    )
                } catch (e: Exception) {
                    throw LlmException.LocalNotReady("On-device generation failed: ${e.message}")
                }
                try {
                    return LlmJson.decode(serializer, text)
                } catch (e: Exception) {
                    lastError = LlmException.InvalidResponse("invalid JSON (${e.message?.take(120)})", e)
                    if (retries-- <= 0) throw lastError
                }
            }
        } finally {
            _phase.value = LocalEnginePhase.Idle
        }
    }

    suspend fun unload() {
        engine.unload()
        _phase.value = LocalEnginePhase.Idle
    }
}
