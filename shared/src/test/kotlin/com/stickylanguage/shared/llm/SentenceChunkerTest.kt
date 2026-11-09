package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentencePair
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
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
    fun translateEachSentence_makes_one_call_per_sentence_and_keeps_pairs_in_order() = runTest {
        val calls = mutableListOf<String>()
        val result = SentenceChunker.translateEachSentence("가. 나.", Language.KOREAN, Language.ENGLISH, 2, 1) { sentence, _ ->
            calls += sentence
            "T($sentence)"
        }
        assertEquals(listOf("가.", "나."), calls)
        assertEquals(listOf(SentencePair("가.", "T(가.)"), SentencePair("나.", "T(나.)")), result.sentences)
        assertEquals("T(가.) T(나.)", result.translation)
        assertEquals("가. 나.", result.sourceText)
    }

    @Test
    fun punctuation_only_pieces_skip_the_model() = runTest {
        val calls = mutableListOf<String>()
        val result = SentenceChunker.translateEachSentence("가. ... 나.", Language.KOREAN, Language.ENGLISH, 2, 1) { sentence, _ ->
            calls += sentence
            "T"
        }
        assertEquals(listOf("가.", "나."), calls)
        assertEquals("T ... T", result.translation)
    }

    @Test
    fun echoed_prompt_markers_are_removed() = runTest {
        val result = SentenceChunker.translateEachSentence("가.", Language.KOREAN, Language.ENGLISH, 2, 1) { _, _ -> "<<<\nHello.\n>>>" }
        assertEquals("Hello.", result.translation)
    }

    @Test
    fun each_sentence_gets_up_to_two_previous_sentences_as_context() = runTest {
        val context = mutableListOf<List<String>>()
        SentenceChunker.translateEachSentence("가. ... 나. 다. 라.", Language.KOREAN, Language.ENGLISH, 2, 1) { _, previous ->
            context += previous
            "T"
        }
        assertEquals(
            listOf(emptyList(), listOf("가."), listOf("가.", "나."), listOf("나.", "다.")),
            context,
        )
    }

    @Test
    fun context_size_is_configurable() = runTest {
        val context = mutableListOf<List<String>>()
        SentenceChunker.translateEachSentence("가. 나. 다.", Language.KOREAN, Language.ENGLISH, 1, 1) { _, previous ->
            context += previous
            "T"
        }
        assertEquals(listOf(emptyList(), listOf("가."), listOf("나.")), context)
    }

    @Test
    fun at_most_the_allowed_calls_run_at_once_and_order_is_kept() = runTest {
        var active = 0
        var peak = 0
        val result = SentenceChunker.translateEachSentence("가. 나. 다. 라. 마.", Language.KOREAN, Language.ENGLISH, 2, 3) { sentence, _ ->
            active++
            peak = maxOf(peak, active)
            delay(10)
            active--
            "T($sentence)"
        }
        assertEquals(3, peak)
        assertEquals("T(가.) T(나.) T(다.) T(라.) T(마.)", result.translation)
    }
}
