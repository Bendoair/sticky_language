package com.stickylanguage.shared.anki

import com.stickylanguage.shared.model.Chunk
import com.stickylanguage.shared.model.Token

/**
 * Where exported notes should go.
 */
enum class ExportTarget(val label: String) {
    AUTO("AnkiDroid if installed, otherwise .apkg file"),
    ANKIDROID("AnkiDroid"),
    APKG_FILE(".apkg file"),
}

/**
 * Note types the app can produce. Field order is the Anki field order.
 */
enum class AnkiNoteType(val label: String, val modelName: String, val fields: List<String>) {
    BASIC(
        label = "Basic",
        modelName = "Basic",
        fields = listOf("Front", "Back"),
    ),
    STICKY(
        label = "Sticky Language (word + reading + sentence)",
        modelName = "Sticky Language",
        fields = listOf("Word", "Reading", "Meaning", "Sentence", "Grammar"),
    );

    companion object {
        fun byName(name: String?): AnkiNoteType = entries.firstOrNull { it.name == name } ?: STICKY
    }
}

/** Default Anki settings from the Settings screen. */
data class AnkiDefaults(
    val deckName: String = "Sticky Language",
    val noteType: AnkiNoteType = AnkiNoteType.STICKY,
    val includeSentence: Boolean = true,
    val exportTarget: ExportTarget = ExportTarget.AUTO,
    val tags: List<String> = listOf("sticky-language"),
)

/**
 * One queued flashcard in the export heap. Nullable overrides fall back to [AnkiDefaults].
 */
data class ExportNote(
    val id: Long = 0,
    val word: String,
    val reading: String = "",
    val meaning: String,
    val grammar: String = "",
    val sentence: String = "",
    val sentenceTranslation: String = "",
    val languageCode: String = "",
    val deckOverride: String? = null,
    val noteTypeOverride: AnkiNoteType? = null,
    val includeSentenceOverride: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun deck(defaults: AnkiDefaults) = deckOverride?.takeIf { it.isNotBlank() } ?: defaults.deckName
    fun noteType(defaults: AnkiDefaults) = noteTypeOverride ?: defaults.noteType
    fun includeSentence(defaults: AnkiDefaults) = includeSentenceOverride ?: defaults.includeSentence

    companion object {
        fun fromToken(token: Token, sentence: String, sentenceTranslation: String, languageCode: String) = ExportNote(
            word = token.lemma.ifBlank { token.surface },
            reading = token.reading,
            meaning = token.meanings.joinToString("; ").ifBlank { token.surface },
            grammar = listOf(token.pos, token.grammar).filter { it.isNotBlank() }.joinToString(" — "),
            sentence = sentence,
            sentenceTranslation = sentenceTranslation,
            languageCode = languageCode,
        )

        fun fromChunk(chunk: Chunk, sentence: String, sentenceTranslation: String, languageCode: String) = ExportNote(
            word = chunk.lemma.ifBlank { chunk.text },
            reading = chunk.reading,
            meaning = chunk.meaning.ifBlank { chunk.morph }.ifBlank { chunk.text },
            grammar = listOf(chunk.role, chunk.morph, chunk.explanation).filter { it.isNotBlank() }.joinToString(" — "),
            sentence = sentence,
            sentenceTranslation = sentenceTranslation,
            languageCode = languageCode,
        )
    }
}

/**
 * Maps a note to concrete Anki field values for its (effective) note type.
 */
object AnkiFieldMapper {

    fun fields(note: ExportNote, defaults: AnkiDefaults): List<String> {
        val includeSentence = note.includeSentence(defaults)
        val sentence = if (includeSentence) highlight(note.sentence, note.word) else ""
        val sentenceTr = if (includeSentence) note.sentenceTranslation else ""
        return when (note.noteType(defaults)) {
            AnkiNoteType.BASIC -> {
                val back = buildString {
                    if (note.reading.isNotBlank()) append("<i>").append(note.reading.escape()).append("</i><br>")
                    append(note.meaning.escape())
                    if (note.grammar.isNotBlank()) append("<br><small>").append(note.grammar.escape()).append("</small>")
                    if (sentence.isNotBlank()) {
                        append("<hr>").append(sentence)
                        if (sentenceTr.isNotBlank()) append("<br><small>").append(sentenceTr.escape()).append("</small>")
                    }
                }
                listOf(note.word.escape(), back)
            }

            AnkiNoteType.STICKY -> listOf(
                note.word.escape(),
                note.reading.escape(),
                note.meaning.escape(),
                if (sentence.isBlank()) "" else {
                    sentence + if (sentenceTr.isNotBlank()) "<br><small>${sentenceTr.escape()}</small>" else ""
                },
                note.grammar.escape(),
            )
        }
    }

    private fun highlight(sentence: String, word: String): String {
        val safe = sentence.escape()
        if (word.isBlank()) return safe
        val idx = safe.indexOf(word.escape())
        if (idx < 0) return safe
        val w = word.escape()
        return safe.substring(0, idx) + "<b>" + w + "</b>" + safe.substring(idx + w.length)
    }

    private fun String.escape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
}
