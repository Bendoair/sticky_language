package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.WordDetails
import com.stickylanguage.shared.model.WordQuery
import kotlinx.serialization.Serializable

/** Wire format of the word-lookup call. */
@Serializable
data class WordDetailsBatch(
    val words: List<WordDetails> = emptyList(),
)

/**
 * Returns one entry per query, in query order. Matches by position when the counts agree and by
 * surface otherwise. Throws if a word has no usable details, so a blank result is never shown or exported.
 */
fun alignWordDetails(queries: List<WordQuery>, returned: List<WordDetails>): List<WordDetails> =
    queries.mapIndexed { index, query ->
        val match = if (returned.size == queries.size) returned[index] else returned.firstOrNull { it.surface == query.surface }
        match?.takeIf { it.meanings.isNotEmpty() }
            ?: throw LlmException.InvalidResponse("no details returned for \"${query.surface}\"")
    }
