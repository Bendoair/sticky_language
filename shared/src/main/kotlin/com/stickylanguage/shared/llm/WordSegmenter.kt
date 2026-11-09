package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.Script
import com.stickylanguage.shared.model.Token
import java.text.BreakIterator
import java.util.Locale

/**
 * Splits source text into tappable words on the device, so the translate call doesn't have to.
 *
 * Japanese and Chinese have no spaces and use [BreakIterator] (dictionary-based on Android).
 * Every other script splits on whitespace. That includes Korean, on purpose: a spaced unit keeps
 * its attached particles or endings (e.g. "도서관에"), which is what the word card explains.
 */
object WordSegmenter {
    private val spacedUnit = Regex("""[^\s\p{Z}]+""")

    /**
     * @param sentences sentence texts from the translation, used to fill [Token.sentenceIndex].
     * A token in no located sentence gets index -1.
     */
    fun tokenize(text: String, language: Language, sentences: List<String>): List<Token> {
        val spans = when (language.script) {
            Script.JAPANESE, Script.CHINESE -> dictionarySpans(text, language)
            else -> spacedSpans(text)
        }
        val ranges = sentenceRanges(text, sentences)
        return spans.map { span ->
            Token(
                surface = text.substring(span),
                sentenceIndex = ranges.firstOrNull { span.first in it.first }?.second ?: -1,
            )
        }
    }

    private fun spacedSpans(text: String): List<IntRange> =
        spacedUnit.findAll(text).mapNotNull { match ->
            var start = match.range.first
            var end = match.range.last + 1
            while (start < end && !isWordChar(text.codePointAt(start))) {
                start += Character.charCount(text.codePointAt(start))
            }
            while (end > start && !isWordChar(text.codePointBefore(end))) {
                end -= Character.charCount(text.codePointBefore(end))
            }
            if (start < end) start until end else null
        }.toList()

    private fun dictionarySpans(text: String, language: Language): List<IntRange> {
        val breaks = BreakIterator.getWordInstance(Locale.forLanguageTag(language.code))
        breaks.setText(text)
        val spans = mutableListOf<IntRange>()
        var start = breaks.first()
        var end = breaks.next()
        while (end != BreakIterator.DONE) {
            if (text.substring(start, end).codePoints().anyMatch(::isWordChar)) spans += start until end
            start = end
            end = breaks.next()
        }
        return spans
    }

    /** Letters, digits and combining marks (needed to keep Devanagari vowel signs attached). */
    private fun isWordChar(codePoint: Int): Boolean =
        Character.isLetterOrDigit(codePoint) || when (Character.getType(codePoint).toByte()) {
            Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
            else -> false
        }

    /** Finds each sentence in order; one that can't be found exactly is skipped. */
    private fun sentenceRanges(text: String, sentences: List<String>): List<Pair<IntRange, Int>> {
        var cursor = 0
        return sentences.mapIndexedNotNull { index, sentence ->
            val trimmed = sentence.trim()
            val start = if (trimmed.isEmpty()) -1 else text.indexOf(trimmed, cursor)
            if (start < 0) {
                null
            } else {
                cursor = start + trimmed.length
                (start until cursor) to index
            }
        }
    }
}
