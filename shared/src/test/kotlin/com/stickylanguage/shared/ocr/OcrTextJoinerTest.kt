package com.stickylanguage.shared.ocr

import com.stickylanguage.shared.llm.SentenceChunker
import com.stickylanguage.shared.model.Script
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextJoinerTest {
    private fun join(script: Script, vararg lines: String) = OcrTextJoiner.join(listOf(lines.toList()), script)

    @Test
    fun wrapped_lines_are_joined_with_a_space() {
        assertEquals("The quick brown fox", join(Script.LATIN, "The quick", "brown fox"))
        assertEquals("저는 도서관에 갑니다.", join(Script.KOREAN, "저는 도서관에", "갑니다."))
    }

    @Test
    fun japanese_and_chinese_are_joined_without_a_space() {
        assertEquals("今日は天気です。", join(Script.JAPANESE, "今日は", "天気です。"))
        assertEquals("我去图书馆。", join(Script.CHINESE, "我去", "图书馆。"))
    }

    @Test
    fun hyphenated_line_end_rejoins_the_word() {
        assertEquals("an example here", join(Script.LATIN, "an exam-", "ple here"))
    }

    @Test
    fun hyphen_before_a_capital_or_a_non_letter_is_kept() {
        assertEquals("North- South", join(Script.LATIN, "North-", "South"))
        assertEquals("wait - and see", join(Script.LATIN, "wait -", "and see"))
    }

    @Test
    fun blocks_stay_on_separate_lines_and_blanks_are_dropped() {
        val blocks = listOf(listOf(" First ", "", "block"), emptyList(), listOf("Second"))
        assertEquals("First block\nSecond", OcrTextJoiner.join(blocks, Script.LATIN))
    }

    @Test
    fun wrapped_sentence_reaches_the_chunker_whole() {
        val text = join(Script.LATIN, "This sentence wraps", "over two lines. Another", "one follows.")
        assertEquals(
            listOf("This sentence wraps over two lines.", "Another one follows."),
            SentenceChunker.split(text),
        )
    }
}
