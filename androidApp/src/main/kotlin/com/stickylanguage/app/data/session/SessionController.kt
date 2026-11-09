package com.stickylanguage.app.data.session

import android.net.Uri
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.llm.local.LocalEnginePhase
import com.stickylanguage.app.llm.local.LocalLlmBackend
import com.stickylanguage.app.ocr.OcrService
import com.stickylanguage.shared.llm.LlmBackend
import com.stickylanguage.shared.llm.LlmException
import com.stickylanguage.shared.llm.WordSegmenter
import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.TranslationResult
import com.stickylanguage.shared.model.WordQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State of the current reading session, shared by Scan / Reader / Grammar screens.
 */
data class SessionState(
    val sourceText: String = "",
    val ocrInProgress: Boolean = false,
    val ocrError: String? = null,

    val translation: TranslationResult? = null,
    /** Source text the [translation] was produced for; used to detect stale results after edits. */
    val translatedSource: String? = null,
    val translating: Boolean = false,
    val translateError: String? = null,
    val localPhase: LocalEnginePhase = LocalEnginePhase.Idle,

    /** Index in `translation.tokens` of the word whose details are loading. */
    val lookingUpWord: Int? = null,
    val wordLookupError: String? = null,

    val breakdown: BreakdownResult? = null,
    val breakdownSource: String? = null,
    val breakingDown: Boolean = false,
    val breakdownError: String? = null,
) {
    val translationIsStale: Boolean get() = translation != null && translatedSource != sourceText
    val breakdownIsStale: Boolean get() = breakdown != null && breakdownSource != sourceText
    val canTranslate: Boolean get() = sourceText.isNotBlank() && !translating && !ocrInProgress
}

/**
 * Owns the session and the explicit LLM triggers. Nothing here runs on text edits;
 * every network / local inference call corresponds to a user action.
 */
@Singleton
class SessionController @Inject constructor(
    private val ocr: OcrService,
    private val llm: LlmBackend,
    private val local: LocalLlmBackend,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    private var ocrJob: Job? = null
    private var translateJob: Job? = null
    private var breakdownJob: Job? = null
    private var wordJob: Job? = null
    private var phaseJob: Job? = null

    init {
        phaseJob = scope.launch {
            local.phase.collect { phase ->
                _state.update { it.copy(localPhase = phase) }
            }
        }
    }

    /** "Type" entry point: empty editable source, no network. */
    fun startEmpty() {
        cancelAll()
        _state.value = SessionState(localPhase = local.phase.value)
    }

    /**
     * Scan / Upload entry point: run on-device OCR, then one translate call.
     * The shutter / picker confirmation is the user action that authorises the call.
     */
    fun startFromImage(uri: Uri) {
        cancelAll()
        _state.value = SessionState(ocrInProgress = true, localPhase = local.phase.value)
        ocrJob = scope.launch {
            val learned = settings.current().learned
            val result = runCatching { ocr.recognize(uri, learned.script) }
            result.onSuccess { text ->
                _state.update { it.copy(sourceText = text, ocrInProgress = false) }
                if (text.isNotBlank()) translate() else {
                    _state.update { it.copy(ocrError = "No text found in the image. You can type it instead.") }
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _state.update { it.copy(ocrInProgress = false, ocrError = e.message ?: "OCR failed") }
            }
        }
    }

    /** Called on every keystroke; only updates text (and stale flags). No network. */
    fun updateSource(text: String) {
        _state.update { it.copy(sourceText = text, ocrError = null) }
    }

    /** Explicit Translate button (or OCR completion). */
    fun translate() {
        val source = _state.value.sourceText.trim()
        if (source.isBlank() || _state.value.translating) return
        translateJob?.cancel()
        cancelWordLookup()
        _state.update { it.copy(translating = true, translateError = null) }
        translateJob = scope.launch {
            val s = settings.current()
            try {
                val translated = llm.translate(source, s.learned, s.explanation)
                val result = translated.copy(
                    tokens = WordSegmenter.tokenize(source, s.learned, translated.sentences.map { it.text }),
                )
                _state.update {
                    it.copy(
                        translation = result,
                        translatedSource = it.sourceText,
                        translating = false,
                        // A new translation invalidates any older breakdown for different text.
                        breakdown = if (it.breakdownSource == it.sourceText) it.breakdown else null,
                        breakdownSource = if (it.breakdownSource == it.sourceText) it.breakdownSource else null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlmException) {
                _state.update { it.copy(translating = false, translateError = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(translating = false, translateError = e.message ?: "Translation failed") }
            }
        }
    }

    /** Word tap. Fetches details once; a word that already has them makes no call. */
    fun requestWord(index: Int) {
        val token = _state.value.translation?.tokens?.getOrNull(index) ?: return
        if (token.hasDetails) return
        cancelWordLookup()
        _state.update { it.copy(lookingUpWord = index) }
        wordJob = scope.launch {
            try {
                fetchMissingWordDetails(listOf(index))
                _state.update { it.copy(lookingUpWord = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(lookingUpWord = null, wordLookupError = e.message ?: "Word lookup failed") }
            }
        }
    }

    fun cancelWordLookup() {
        wordJob?.cancel()
        wordJob = null
        _state.update { it.copy(lookingUpWord = null, wordLookupError = null) }
    }

    /** Stores details on the tokens at [indices], skipping those already loaded. Throws on failure. */
    suspend fun fetchMissingWordDetails(indices: Collection<Int>) {
        val start = _state.value
        val translation = start.translation ?: return
        val missing = indices.filter { translation.tokens.getOrNull(it)?.hasDetails == false }
        if (missing.isEmpty()) return
        val queries = missing.map { i ->
            val token = translation.tokens[i]
            WordQuery(token.surface, translation.sentenceOf(token)?.text ?: translation.sourceText)
        }
        val s = settings.current()
        val details = missing.zip(llm.lookupWords(queries, s.learned, s.explanation)).toMap()
        _state.update { current ->
            // Indices only apply to the translation they were requested for.
            val latest = current.translation
            if (latest == null || current.translatedSource != start.translatedSource) return@update current
            current.copy(
                translation = latest.copy(
                    tokens = latest.tokens.mapIndexed { i, token -> details[i]?.let(token::withDetails) ?: token },
                ),
            )
        }
    }

    /** Explicit Grammar Breakdown button. Reuses a fresh breakdown for the same source. */
    fun requestBreakdown(force: Boolean = false) {
        val st = _state.value
        val source = st.sourceText.trim()
        if (source.isBlank() || st.breakingDown) return
        if (!force && st.breakdown != null && st.breakdownSource == st.sourceText) return
        breakdownJob?.cancel()
        _state.update { it.copy(breakingDown = true, breakdownError = null) }
        breakdownJob = scope.launch {
            val s = settings.current()
            try {
                val result = llm.breakdown(source, s.learned, s.explanation)
                _state.update { it.copy(breakdown = result, breakdownSource = it.sourceText, breakingDown = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlmException) {
                _state.update { it.copy(breakingDown = false, breakdownError = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(breakingDown = false, breakdownError = e.message ?: "Breakdown failed") }
            }
        }
    }

    fun clearErrors() {
        _state.update { it.copy(ocrError = null, translateError = null, breakdownError = null) }
    }

    private fun cancelAll() {
        ocrJob?.cancel(); translateJob?.cancel(); breakdownJob?.cancel(); wordJob?.cancel()
    }
}
