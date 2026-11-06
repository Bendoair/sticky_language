package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.Token
import com.stickylanguage.shared.model.TranslationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceChunkerTest {
    @Test
    fun split_on_punctuation_and_newlines() {
        val parts = SentenceChunker.split("Hello. World!\n다시 한 번?")
        assertEquals(listOf("Hello.", "World!", "다시 한 번?"), parts)
    }

    @Test
    fun shouldChunk_for_multi_sentence() {
        assertTrue(SentenceChunker.shouldChunk("One. Two."))
        assertFalse(SentenceChunker.shouldChunk("Short"))
    }

    @Test
    fun mergeTranslations_offsets_sentence_index() {
        val a = TranslationResult(
            translation = "A",
            tokens = listOf(Token(surface = "가", sentenceIndex = 0)),
            sentences = listOf(SentencePair("가.", "A.")),
            sourceText = "가.",
        )
        val b = TranslationResult(
            translation = "B",
            tokens = listOf(Token(surface = "나", sentenceIndex = 0)),
            sentences = listOf(SentencePair("나.", "B.")),
            sourceText = "나.",
        )
        val merged = SentenceChunker.mergeTranslations("가. 나.", Language.KOREAN, Language.ENGLISH, listOf(a, b))
        assertEquals("A B", merged.translation)
        assertEquals(2, merged.sentences.size)
        assertEquals(0, merged.tokens[0].sentenceIndex)
        assertEquals(1, merged.tokens[1].sentenceIndex)
    }
}
