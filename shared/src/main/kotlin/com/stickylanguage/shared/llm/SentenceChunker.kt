package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceBreakdown
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.TranslationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Splits OCR / typed source into sentence-sized chunks. Translation sends one LLM call per
 * chunk; on-device breakdowns do too, as local models can't reliably emit a huge JSON blob.
 */
object SentenceChunker {
    private val splitter = Regex("""(?<=[.!?。！？…])\s+|\n+""")

    /** The delimiters around the text in the translate prompts, which models sometimes copy. */
    private val promptMarkers = Regex("<<<|>>>")

    fun split(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        val parts = splitter.split(trimmed).map { it.trim() }.filter { it.isNotEmpty() }
        return parts.ifEmpty { listOf(trimmed) }
    }

    /** Chunk when there is more than one sentence or the text is long. */
    fun shouldChunk(text: String, charThreshold: Int = 180): Boolean {
        val parts = split(text)
        return parts.size > 1 || text.length > charThreshold
    }

    /**
     * Translates [source] one sentence at a time, in order, pairing each result with its sentence.
     * [translateSentence] receives the sentence and up to [contextSentences] sentences before it,
     * with at most [concurrency] calls running at once. If one call fails, the others are cancelled.
     */
    suspend fun translateEachSentence(
        source: String,
        learned: Language,
        explain: Language,
        contextSentences: Int,
        concurrency: Int,
        translateSentence: suspend (sentence: String, previous: List<String>) -> String,
    ): TranslationResult {
        val parts = split(source)
        val permits = Semaphore(concurrency)
        val sentences = coroutineScope {
            parts.mapIndexed { index, sentence ->
                val previous = parts.subList(0, index).filter(::hasLetters).takeLast(contextSentences)
                async {
                    permits.withPermit {
                        SentencePair(text = sentence, translation = translated(sentence, previous, translateSentence))
                    }
                }
            }.awaitAll()
        }
        return TranslationResult(
            sourceLanguage = learned.code,
            explanationLanguage = explain.code,
            sourceText = source,
            translation = sentences.joinToString(" ") { it.translation },
            sentences = sentences,
        )
    }

    /** Punctuation-only pieces such as "..." are kept as they are; models tend to echo the prompt for them. */
    private suspend fun translated(
        sentence: String,
        previous: List<String>,
        translateSentence: suspend (String, List<String>) -> String,
    ): String =
        if (!hasLetters(sentence)) sentence
        else translateSentence(sentence, previous).replace(promptMarkers, "").trim()

    private fun hasLetters(text: String): Boolean = text.any { it.isLetterOrDigit() }

    fun mergeBreakdowns(
        learned: Language,
        explain: Language,
        parts: List<BreakdownResult>,
    ): BreakdownResult {
        val sentences = mutableListOf<SentenceBreakdown>()
        for (part in parts) sentences += part.sentences
        return BreakdownResult(
            sourceLanguage = learned.code,
            explanationLanguage = explain.code,
            sentences = sentences,
        )
    }
}
