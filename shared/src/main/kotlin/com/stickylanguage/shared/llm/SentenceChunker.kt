package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentenceBreakdown
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.TranslationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
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
     * Translates [source] one sentence at a time, pairing each result with its sentence, and returns
     * the complete result. See [translateEachSentenceProgressive] for the arguments.
     */
    suspend fun translateEachSentence(
        source: String,
        learned: Language,
        explain: Language,
        contextSentences: Int,
        concurrency: Int,
        translateSentence: suspend (sentence: String, previous: List<String>) -> String,
    ): TranslationResult =
        translateEachSentenceProgressive(source, learned, explain, contextSentences, concurrency, null, translateSentence)
            .last()

    /**
     * Translates [source] one sentence at a time, pairing each result with its sentence.
     *
     * Emits a snapshot right away in which sentences still to translate have a null translation,
     * then a new snapshot each time one finishes, in the order they finish. The last emission is complete.
     * [translateSentence] receives the sentence and up to [contextSentences] sentences before it,
     * with at most [concurrency] calls running at once. If one call fails, the others are cancelled
     * and the flow throws; the snapshots already emitted keep the finished sentences.
     *
     * Translations in [resumeFrom] are reused, and not requested again, when it was made for the same
     * [source] and split into the same sentences; otherwise it is ignored.
     */
    fun translateEachSentenceProgressive(
        source: String,
        learned: Language,
        explain: Language,
        contextSentences: Int,
        concurrency: Int,
        resumeFrom: TranslationResult?,
        translateSentence: suspend (sentence: String, previous: List<String>) -> String,
    ): Flow<TranslationResult> {
        val parts = split(source)
        val reusable = resumeFrom
            ?.takeIf { it.sourceText == source && it.sentences.map(SentencePair::text) == parts }
            ?.sentences
        // Punctuation-only pieces such as "..." are kept as they are; models tend to echo the prompt for them.
        val initial: List<String?> = parts.mapIndexed { index, sentence ->
            if (!hasLetters(sentence)) sentence else reusable?.get(index)?.translation
        }
        val pending = initial.indices.filter { initial[it] == null }

        return channelFlow {
            val permits = Semaphore(concurrency)
            for (index in pending) {
                val previous = parts.subList(0, index).filter(::hasLetters).takeLast(contextSentences)
                launch {
                    permits.withPermit {
                        val text = translateSentence(parts[index], previous).replace(promptMarkers, "").trim()
                        send(index to text)
                    }
                }
            }
        }
            .runningFold(initial) { translations, (index, text) ->
                translations.toMutableList().apply { set(index, text) }
            }
            .map { translations ->
                TranslationResult(
                    sourceLanguage = learned.code,
                    explanationLanguage = explain.code,
                    sourceText = source,
                    translation = translations.filterNotNull().joinToString(" "),
                    sentences = parts.zip(translations) { text, translation -> SentencePair(text, translation) },
                )
            }
    }

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
