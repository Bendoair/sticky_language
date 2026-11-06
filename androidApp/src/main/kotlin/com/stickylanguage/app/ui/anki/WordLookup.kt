package com.stickylanguage.app.ui.anki

import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.model.Chunk
import com.stickylanguage.shared.model.Token
import com.stickylanguage.shared.model.TranslationResult

/**
 * Everything the word pop-up needs, built from either a Reader token or a Grammar chunk.
 */
data class WordLookup(
    val surface: String,
    val lemma: String,
    val reading: String,
    val pos: String,
    val meanings: List<String>,
    val grammar: String,
    val example: String,
    val sentence: String,
    val sentenceTranslation: String,
    val languageCode: String,
) {
    fun toExportNote(): ExportNote = ExportNote(
        word = lemma.ifBlank { surface },
        reading = reading,
        meaning = meanings.joinToString("; ").ifBlank { surface },
        grammar = listOf(pos, grammar).filter { it.isNotBlank() }.joinToString(" — "),
        sentence = sentence,
        sentenceTranslation = sentenceTranslation,
        languageCode = languageCode,
    )

    companion object {
        fun fromToken(token: Token, translation: TranslationResult?, languageCode: String): WordLookup {
            val pair = translation?.sentences?.getOrNull(token.sentenceIndex)
                ?: translation?.sentences?.firstOrNull { it.text.contains(token.surface) }
            return WordLookup(
                surface = token.surface,
                lemma = token.lemma,
                reading = token.reading,
                pos = token.pos,
                meanings = token.meanings,
                grammar = token.grammar,
                example = token.example,
                sentence = pair?.text ?: translation?.sourceText.orEmpty(),
                sentenceTranslation = pair?.translation.orEmpty(),
                languageCode = languageCode,
            )
        }

        fun fromChunk(chunk: Chunk, sentence: String, sentenceTranslation: String, languageCode: String) = WordLookup(
            surface = chunk.text,
            lemma = chunk.lemma,
            reading = chunk.reading,
            pos = chunk.role,
            meanings = listOfNotNull(chunk.meaning.takeIf { it.isNotBlank() }, chunk.morph.takeIf { it.isNotBlank() }),
            grammar = chunk.explanation,
            example = "",
            sentence = sentence,
            sentenceTranslation = sentenceTranslation,
            languageCode = languageCode,
        )
    }
}
