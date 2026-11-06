package com.stickylanguage.shared.llm

import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.TranslationResult

/**
 * Provider-agnostic LLM surface used by the app. Cloud and on-device backends both implement this.
 */
interface LlmBackend {
    suspend fun translate(source: String, learned: Language, explain: Language): TranslationResult
    suspend fun breakdown(source: String, learned: Language, explain: Language): BreakdownResult
}

enum class LlmProvider(val label: String) {
    CLOUD("Cloud"),
    LOCAL("On-device"),
}
