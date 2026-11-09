package com.stickylanguage.app.llm

import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.llm.local.LocalLlmBackend
import com.stickylanguage.shared.llm.LlmBackend
import com.stickylanguage.shared.llm.LlmProvider
import com.stickylanguage.shared.llm.RemoteLlmBackend
import com.stickylanguage.shared.model.BreakdownResult
import com.stickylanguage.shared.model.Language
import com.stickylanguage.shared.model.TranslationResult
import com.stickylanguage.shared.model.WordDetails
import com.stickylanguage.shared.model.WordQuery
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes translate / breakdown to cloud or on-device based on current Settings.
 */
@Singleton
class RoutingLlmBackend @Inject constructor(
    private val settings: SettingsRepository,
    private val remote: RemoteLlmBackend,
    private val local: LocalLlmBackend,
) : LlmBackend {

    private suspend fun active(): LlmBackend =
        when (settings.current().llmProvider) {
            LlmProvider.CLOUD -> remote
            LlmProvider.LOCAL -> local
        }

    override suspend fun translate(source: String, learned: Language, explain: Language): TranslationResult =
        active().translate(source, learned, explain)

    override suspend fun breakdown(source: String, learned: Language, explain: Language): BreakdownResult =
        active().breakdown(source, learned, explain)

    override suspend fun lookupWords(queries: List<WordQuery>, learned: Language, explain: Language): List<WordDetails> =
        active().lookupWords(queries, learned, explain)
}
