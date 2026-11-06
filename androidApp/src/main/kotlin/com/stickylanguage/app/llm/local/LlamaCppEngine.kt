package com.stickylanguage.app.llm.local

import android.util.Log
import com.thalzeronlabs.llama.ChatMessage
import com.thalzeronlabs.llama.Grammar
import com.thalzeronlabs.llama.LlamaModel
import com.thalzeronlabs.llama.LlamaSession
import com.thalzeronlabs.llama.ModelParams
import com.thalzeronlabs.llama.SessionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin lifecycle wrapper around llama-kotlin. All native work stays on [Dispatchers.IO]
 * behind a mutex so only one generation runs at a time.
 */
@Singleton
class LlamaCppEngine @Inject constructor() {
    private val mutex = Mutex()
    private val loaded = AtomicReference<Loaded?>(null)

    data class Loaded(
        val modelId: String,
        val model: LlamaModel,
        val session: LlamaSession,
    )

    val loadedModelId: String? get() = loaded.get()?.modelId

    suspend fun ensureLoaded(modelId: String, path: File) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loaded.get()
            if (current?.modelId == modelId) return@withLock
            releaseLocked()
            Log.i(TAG, "Loading model $modelId from ${path.absolutePath}")
            val model = LlamaModel.load(path, ModelParams(useMmap = true))
            val session = model.newSession(
                SessionParams(
                    contextSize = CONTEXT_SIZE,
                    temperature = 0.2f,
                    topK = 40,
                    topP = 0.95f,
                    minP = 0.05f,
                    seed = -1,
                    threads = 0,
                ),
            )
            loaded.set(Loaded(modelId, model, session))
        }
    }

    suspend fun complete(
        system: String,
        user: String,
        jsonSchema: String,
        maxTokens: Int = MAX_TOKENS,
    ): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loaded.get() ?: error("No local model loaded")
            current.session.reset()
            val grammar = Grammar.jsonSchema(jsonSchema)
            val pieces = current.session.chat(
                messages = listOf(
                    ChatMessage.system(system),
                    ChatMessage.user(user),
                ),
                maxTokens = maxTokens,
                grammar = grammar,
                temperature = 0.2f,
            ).toList()
            pieces.joinToString("")
        }
    }

    suspend fun unload() = withContext(Dispatchers.IO) {
        mutex.withLock { releaseLocked() }
    }

    fun unloadBlocking() {
        // Best-effort for onTrimMemory / process teardown.
        try {
            val current = loaded.getAndSet(null) ?: return
            runCatching { current.session.close() }
            runCatching { current.model.close() }
            Log.i(TAG, "Unloaded model ${current.modelId}")
        } catch (e: Exception) {
            Log.w(TAG, "Unload failed", e)
        }
    }

    private fun releaseLocked() {
        val current = loaded.getAndSet(null) ?: return
        runCatching { current.session.close() }
        runCatching { current.model.close() }
        Log.i(TAG, "Released model ${current.modelId}")
    }

    companion object {
        private const val TAG = "LlamaCppEngine"
        const val CONTEXT_SIZE = 4096
        const val MAX_TOKENS = 2048
    }
}
