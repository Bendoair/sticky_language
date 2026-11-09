package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.WordDetails
import com.stickylanguage.shared.model.WordQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WordSegmenterTest {
    private fun surfaces(text: String, language: Language) =
        WordSegmenter.tokenize(text, language, emptyList()).map { it.surface }

    @Test
    fun korean_keeps_attached_particles_and_drops_punctuation() {
        assertEquals(listOf("저는", "도서관에", "갑니다"), surfaces("저는 도서관에 갑니다.", Language.KOREAN))
    }

    @Test
    fun latin_trims_edge_punctuation_only() {
        assertEquals(listOf("Qué", "tal", "l'ami"), surfaces("¿Qué tal, «l'ami»?", Language.SPANISH))
    }

    @Test
    fun devanagari_keeps_combining_marks() {
        assertEquals(listOf("नमस्ते", "दुनिया"), surfaces("नमस्ते दुनिया।", Language.HINDI))
    }

    @Test
    fun standalone_punctuation_is_not_a_token() {
        assertEquals(listOf("one", "two"), surfaces("one — two", Language.ENGLISH))
    }

    /** The JVM's word splits differ from Android's, so only the invariants are checked here. */
    @Test
    fun japanese_tokens_cover_the_text_without_punctuation() {
        assertEquals("今日は天気です", surfaces("今日は、天気です。", Language.JAPANESE).joinToString(""))
    }

    @Test
    fun tokens_are_mapped_to_their_sentence() {
        val tokens = WordSegmenter.tokenize("One two. Three.", Language.ENGLISH, listOf("One two.", "Three."))
        assertEquals(listOf(0, 0, 1), tokens.map { it.sentenceIndex })
    }

    @Test
    fun sentence_that_cannot_be_located_gives_minus_one() {
        val tokens = WordSegmenter.tokenize("One. Two.", Language.ENGLISH, listOf("One.", "Changed."))
        assertEquals(listOf(0, -1), tokens.map { it.sentenceIndex })
    }

    @Test
    fun align_falls_back_to_surface_when_counts_differ() {
        val queries = listOf(WordQuery("a", "s"), WordQuery("b", "s"))
        val b = WordDetails(surface = "b", meanings = listOf("bee"))
        val a = WordDetails(surface = "a", meanings = listOf("ay"))
        assertEquals(listOf(a, b), alignWordDetails(queries, listOf(b, a, b)))
    }

    @Test
    fun align_throws_when_a_word_has_no_meaning() {
        val queries = listOf(WordQuery("a", "s"))
        assertThrows(LlmException.InvalidResponse::class.java) {
            alignWordDetails(queries, listOf(WordDetails(surface = "a")))
        }
    }
}
