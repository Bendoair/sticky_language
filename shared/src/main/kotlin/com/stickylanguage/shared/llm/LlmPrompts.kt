package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.WordQuery

/**
 * Versioned prompt templates. Keep all LLM wording here so the Android layer never builds prompts.
 */
object LlmPrompts {
    const val VERSION = 2

    private fun systemBase(learned: Language, explain: Language) = """
        You are Sticky Language, a precise language tutor.
        The learner is studying ${learned.englishName} (${learned.code}) and wants explanations in ${explain.englishName} (${explain.code}).
        Rules:
        - Respond ONLY with a single JSON object that matches the provided schema. No markdown, no code fences, no commentary.
        - Write every explanation, meaning and translation in ${explain.englishName}.
        - Keep the source text exactly as given; never correct or normalise it in "text" / "surface" / "source_text" fields.
        - Be concise and accurate. Prefer the most common meaning in this context.
        - The input may come from OCR and contain small recognition errors; interpret it charitably but do not invent content.
    """.trimIndent()

    fun translateSystem(learned: Language, explain: Language): String = systemBase(learned, explain) + "\n" + """
        Task: translate the text.
        - "translation": a natural, context-aware translation of the whole text.
        - "sentences": split the text into sentences, each with its own translation.
    """.trimIndent()

    fun wordLookupSystem(learned: Language, explain: Language): String = systemBase(learned, explain) + "\n" + """
        Task: explain each listed word as it is used in its sentence.
        - Return one entry per listed word, in the same order. "surface" must be the word exactly as listed.
        - For each word give the dictionary form, reading (romanization) when the script is not Latin, part of speech,
          1-3 meanings, a short grammar note about attached particles/conjugation, and a short example.
    """.trimIndent()

    fun wordLookupUser(words: List<WordQuery>, learned: Language, explain: Language): String = buildString {
        appendLine("Learned language: ${learned.englishName} (${learned.code})")
        appendLine("Explanation language: ${explain.englishName} (${explain.code})")
        appendLine("Words:")
        words.forEachIndexed { i, word ->
            appendLine("${i + 1}. Word: <<<${word.surface}>>> Sentence: <<<${word.sentence}>>>")
        }
    }.trimEnd()

    fun translateUser(source: String, learned: Language, explain: Language): String = """
        Learned language: ${learned.englishName} (${learned.code})
        Explanation language: ${explain.englishName} (${explain.code})
        Source text:
        <<<
        $source
        >>>
    """.trimIndent()

    fun breakdownSystem(learned: Language, explain: Language): String = systemBase(learned, explain) + "\n" + """
        Task: produce a sentence-by-sentence grammar breakdown.
        - Return every sentence of the text in order.
        - "chunks": split each sentence into syntactic chunks (topic, object, time, place, predicate...). Concatenated in order,
          the chunk texts should cover the sentence (whitespace/punctuation may be dropped). Give morphology and a short explanation.
        - "grammar_points": the 1-4 most important grammar patterns of the sentence with a clear explanation and one example.
    """.trimIndent()

    fun breakdownUser(source: String, learned: Language, explain: Language): String =
        translateUser(source, learned, explain)

    /** Appended to the system prompt when the provider only supports plain JSON mode. */
    fun schemaFallbackSuffix(schemaJson: String): String = """

        The JSON object MUST have exactly this JSON Schema (all properties required, no extra keys):
        $schemaJson
    """.trimIndent()
}
