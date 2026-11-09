package com.stickylanguage.shared.ocr

import com.stickylanguage.shared.model.Script

/**
 * Undoes the line wrapping of a scanned page, so a sentence printed over several lines
 * reaches the translator as one sentence.
 */
object OcrTextJoiner {
    private val hyphens = charArrayOf('-', '‐', '­')

    /** @param blocks the recognized text blocks, each a list of printed lines. Blocks stay on separate lines. */
    fun join(blocks: List<List<String>>, script: Script): String =
        blocks.map { lines -> joinLines(lines, script) }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun joinLines(lines: List<String>, script: Script): String =
        lines.map { it.trim() }
            .filter { it.isNotEmpty() }
            .fold("") { joined, line -> if (joined.isEmpty()) line else append(joined, line, script) }

    private fun append(joined: String, line: String, script: Script): String = when {
        script == Script.JAPANESE || script == Script.CHINESE -> joined + line
        script == Script.LATIN && isSplitWord(joined, line) -> joined.dropLast(1) + line
        else -> "$joined $line"
    }

    /** A letter, a line-end hyphen, then a lowercase continuation: "exam-" + "ple". */
    private fun isSplitWord(joined: String, line: String): Boolean =
        joined.length >= 2 &&
            joined.last() in hyphens &&
            joined[joined.length - 2].isLetter() &&
            line.first().isLowerCase()
}
