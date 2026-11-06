package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceBreakdown
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.Token
import com.stickylanguage.shared.model.TranslationResult

/**
 * Splits OCR / typed source into sentence-sized chunks for local models that cannot
 * reliably emit a huge structured JSON blob in one shot.
 */
object SentenceChunker {
    private val splitter = Regex("""(?<=[.!?。！？…])\s+|\n+""")

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

    fun mergeTranslations(
        source: String,
        learned: Language,
        explain: Language,
        parts: List<TranslationResult>,
    ): TranslationResult {
        val sentences = mutableListOf<SentencePair>()
        val tokens = mutableListOf<Token>()
        val translationParts = mutableListOf<String>()
        var sentenceOffset = 0
        for (part in parts) {
            translationParts += part.translation
            for (s in part.sentences) sentences += s
            for (t in part.tokens) tokens += t.copy(sentenceIndex = t.sentenceIndex + sentenceOffset)
            sentenceOffset += part.sentences.size.coerceAtLeast(1)
        }
        return TranslationResult(
            sourceLanguage = learned.code,
            explanationLanguage = explain.code,
            sourceText = source,
            translation = translationParts.joinToString(" "),
            tokens = tokens,
            sentences = sentences.ifEmpty {
                parts.map { SentencePair(text = it.sourceText, translation = it.translation) }
            },
        )
    }

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
