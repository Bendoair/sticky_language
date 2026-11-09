package com.stickylanguage.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured response of the "translate" LLM call. Field names are the wire format
 * (snake_case) so the same classes serve as DTOs and as app models.
 * The LLM doesn't return [tokens]: they are split on the device after the call.
 */
@Serializable
data class TranslationResult(
    @SerialName("source_language") val sourceLanguage: String = "",
    @SerialName("explanation_language") val explanationLanguage: String = "",
    @SerialName("source_text") val sourceText: String = "",
    val translation: String,
    val tokens: List<Token> = emptyList(),
    val sentences: List<SentencePair> = emptyList(),
) {
    fun sentenceOf(token: Token): SentencePair? =
        sentences.getOrNull(token.sentenceIndex)
            ?: sentences.firstOrNull { it.text.contains(token.surface) }
}

/** One word to look up, with the sentence that gives it context. */
data class WordQuery(
    val surface: String,
    val sentence: String,
)

/** Word-card details, fetched only when a word is opened or exported. */
@Serializable
data class WordDetails(
    val surface: String = "",
    val lemma: String = "",
    val reading: String = "",
    val pos: String = "",
    val meanings: List<String> = emptyList(),
    val grammar: String = "",
    val example: String = "",
)

@Serializable
data class Token(
    /** Exact surface form as it appears in the source text (e.g. "도서관에"). */
    val surface: String,
    /** Dictionary form (e.g. "도서관"). */
    val lemma: String = "",
    /** Romanization / reading; empty when not applicable. */
    val reading: String = "",
    /** Part of speech, e.g. "noun + particle". */
    val pos: String = "",
    val meanings: List<String> = emptyList(),
    /** Short note on the grammar attached to this token (particles, conjugation...). */
    val grammar: String = "",
    /** Example sentence in the learned language with translation, may be empty. */
    val example: String = "",
    @SerialName("sentence_index") val sentenceIndex: Int = 0,
) {
    val primaryMeaning: String get() = meanings.firstOrNull().orEmpty()

    val hasDetails: Boolean get() = meanings.isNotEmpty()

    fun withDetails(details: WordDetails): Token = copy(
        lemma = details.lemma,
        reading = details.reading,
        pos = details.pos,
        meanings = details.meanings,
        grammar = details.grammar,
        example = details.example,
    )
}

@Serializable
data class SentencePair(
    val text: String,
    val translation: String,
)

/**
 * Structured response of the "grammar breakdown" LLM call.
 */
@Serializable
data class BreakdownResult(
    @SerialName("source_language") val sourceLanguage: String = "",
    @SerialName("explanation_language") val explanationLanguage: String = "",
    val sentences: List<SentenceBreakdown> = emptyList(),
)

@Serializable
data class SentenceBreakdown(
    val text: String,
    val translation: String,
    val chunks: List<Chunk> = emptyList(),
    @SerialName("grammar_points") val grammarPoints: List<GrammarPoint> = emptyList(),
)

@Serializable
data class Chunk(
    /** Surface text of the chunk (may be more than one word). */
    val text: String,
    /** Role in the sentence: Topic, Object, Predicate, Time... */
    val role: String = "",
    /** Morphological composition, e.g. "도서관 (library) + 에 (to)". */
    val morph: String = "",
    val explanation: String = "",
    /** Dictionary form of the head word, used when exporting to Anki. */
    val lemma: String = "",
    val reading: String = "",
    val meaning: String = "",
)

@Serializable
data class GrammarPoint(
    val name: String,
    val body: String,
)
