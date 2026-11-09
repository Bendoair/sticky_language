package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceBreakdown
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.TranslationResult

/**
 * Splits OCR / typed source into sentence-sized chunks. Translation sends one LLM call per
 * chunk; on-device breakdowns do too, as local models can't reliably emit a huge JSON blob.
 */
object SentenceChunker {
    private val splitter = Regex("""(?<=[.!?。！？…])\s+|\n+""")

    /** The delimiters around the source in [LlmPrompts.translateUser], which models sometimes copy. */
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

    /** Translates [source] one sentence at a time, in order, pairing each result with its sentence. */
    suspend fun translateEachSentence(
        source: String,
        learned: Language,
        explain: Language,
        translateSentence: suspend (String) -> String,
    ): TranslationResult {
        val sentences = split(source).map { SentencePair(text = it, translation = translated(it, translateSentence)) }
        return TranslationResult(
            sourceLanguage = learned.code,
            explanationLanguage = explain.code,
            sourceText = source,
            translation = sentences.joinToString(" ") { it.translation },
            sentences = sentences,
        )
    }

    /** Punctuation-only pieces such as "..." are kept as they are; models tend to echo the prompt for them. */
    private suspend fun translated(sentence: String, translateSentence: suspend (String) -> String): String =
        if (sentence.none { it.isLetterOrDigit() }) sentence
        else translateSentence(sentence).replace(promptMarkers, "").trim()

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
