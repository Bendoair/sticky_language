package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.SentencePair
import com.stickylanguage.shared.model.TranslationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun progressive_first_snapshot_has_every_sentence_pending_except_punctuation() = runTest {
        val first = SentenceChunker.translateEachSentenceProgressive("가. ... 나.", Language.KOREAN, Language.ENGLISH, 2, 1, null) { _, _ -> "T" }
            .toList()
            .first()
        assertEquals(listOf(null, "...", null), first.sentences.map { it.translation })
        assertEquals("...", first.translation)
        assertFalse(first.isComplete)
    }

    @Test
    fun progressive_snapshots_follow_the_order_sentences_finish_in() = runTest {
        val delays = mapOf("가." to 30L, "나." to 10L, "다." to 20L)
        val snapshots = SentenceChunker.translateEachSentenceProgressive("가. 나. 다.", Language.KOREAN, Language.ENGLISH, 2, 3, null) { sentence, _ ->
            delay(delays.getValue(sentence))
            "T($sentence)"
        }.toList()
        assertEquals(
            listOf(
                listOf(null, null, null),
                listOf(null, "T(나.)", null),
                listOf(null, "T(나.)", "T(다.)"),
                listOf("T(가.)", "T(나.)", "T(다.)"),
            ),
            snapshots.map { snapshot -> snapshot.sentences.map { it.translation } },
        )
        assertEquals("T(나.) T(다.)", snapshots[2].translation)
        assertTrue(snapshots.last().isComplete)
    }

    @Test
    fun progressive_emits_finished_sentences_before_a_failure() = runTest {
        val snapshots = mutableListOf<TranslationResult>()
        val failure = runCatching {
            SentenceChunker.translateEachSentenceProgressive("가. 나. 다.", Language.KOREAN, Language.ENGLISH, 2, 1, null) { sentence, _ ->
                if (sentence == "나.") throw IllegalStateException("boom")
                "T($sentence)"
            }.collect { snapshots += it }
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(listOf("T(가.)", null, null), snapshots.last().sentences.map { it.translation })
    }

    @Test
    fun progressive_resume_only_translates_the_missing_sentences() = runTest {
        val partial = TranslationResult(
            sourceText = "가. 나. 다.",
            translation = "T(가.)",
            sentences = listOf(SentencePair("가.", "T(가.)"), SentencePair("나."), SentencePair("다.")),
        )
        val calls = mutableListOf<String>()
        val result = SentenceChunker.translateEachSentenceProgressive("가. 나. 다.", Language.KOREAN, Language.ENGLISH, 2, 1, partial) { sentence, _ ->
            calls += sentence
            "U($sentence)"
        }.last()
        assertEquals(listOf("나.", "다."), calls)
        assertEquals("T(가.) U(나.) U(다.)", result.translation)
    }

    @Test
    fun progressive_resume_is_ignored_for_a_different_source() = runTest {
        val partial = TranslationResult(
            sourceText = "가. 나.",
            translation = "T(가.)",
            sentences = listOf(SentencePair("가.", "T(가.)"), SentencePair("나.")),
        )
        val first = SentenceChunker.translateEachSentenceProgressive("가. 라.", Language.KOREAN, Language.ENGLISH, 2, 1, partial) { _, _ -> "U" }
            .toList()
            .first()
        assertNull(first.sentences.first().translation)
    }
}
