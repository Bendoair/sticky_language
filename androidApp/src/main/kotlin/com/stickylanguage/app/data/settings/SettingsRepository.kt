package com.stickylanguage.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stickylanguage.app.BuildConfig
import com.stickylanguage.app.llm.local.ModelCatalog
import com.stickylanguage.shared.anki.AnkiDefaults
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportTarget
import com.stickylanguage.shared.llm.LlmConfig
import com.stickylanguage.shared.llm.LlmProvider
import com.stickylanguage.shared.model.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val learned: Language = Language.KOREAN,
    val explanation: Language = Language.ENGLISH,
    /** Settings override; blank means "use the build-time default". */
    val apiKeyOverride: String = "",
    val baseUrlOverride: String = "",
    val modelOverride: String = "",
    val llmProvider: LlmProvider = LlmProvider.CLOUD,
    val localModelId: String = ModelCatalog.DEFAULT_ID,
    val anki: AnkiDefaults = AnkiDefaults(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val hasBuildTimeKey: Boolean get() = BuildConfig.LLM_API_KEY.isNotBlank()

    val llmConfig: LlmConfig
        get() = LlmConfig(
            apiKey = apiKeyOverride.ifBlank { BuildConfig.LLM_API_KEY },
            baseUrl = baseUrlOverride.ifBlank { BuildConfig.LLM_BASE_URL },
            model = modelOverride.ifBlank { BuildConfig.LLM_MODEL },
        )
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val learned = stringPreferencesKey("learned_language")
        val explanation = stringPreferencesKey("explanation_language")
        val apiKey = stringPreferencesKey("llm_api_key")
        val baseUrl = stringPreferencesKey("llm_base_url")
        val model = stringPreferencesKey("llm_model")
        val llmProvider = stringPreferencesKey("llm_provider")
        val localModelId = stringPreferencesKey("local_model_id")
        val ankiDeck = stringPreferencesKey("anki_deck")
        val ankiNoteType = stringPreferencesKey("anki_note_type")
        val ankiIncludeSentence = booleanPreferencesKey("anki_include_sentence")
        val ankiExportTarget = stringPreferencesKey("anki_export_target")
        val theme = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            learned = Language.byCode(p[Keys.learned]) ?: Language.KOREAN,
            explanation = Language.byCode(p[Keys.explanation]) ?: Language.ENGLISH,
            apiKeyOverride = p[Keys.apiKey].orEmpty(),
            baseUrlOverride = p[Keys.baseUrl].orEmpty(),
            modelOverride = p[Keys.model].orEmpty(),
            llmProvider = p[Keys.llmProvider]?.let { name ->
                LlmProvider.entries.firstOrNull { it.name == name }
            } ?: LlmProvider.CLOUD,
            localModelId = p[Keys.localModelId]?.takeIf { it.isNotBlank() } ?: ModelCatalog.DEFAULT_ID,
            anki = AnkiDefaults(
                deckName = p[Keys.ankiDeck]?.takeIf { it.isNotBlank() } ?: AnkiDefaults().deckName,
                noteType = AnkiNoteType.byName(p[Keys.ankiNoteType]),
                includeSentence = p[Keys.ankiIncludeSentence] ?: true,
                exportTarget = p[Keys.ankiExportTarget]?.let { v -> ExportTarget.entries.firstOrNull { it.name == v } }
                    ?: ExportTarget.AUTO,
            ),
            themeMode = p[Keys.theme]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
        )
    }

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLearned(language: Language) = edit { it[Keys.learned] = language.code }
    suspend fun setExplanation(language: Language) = edit { it[Keys.explanation] = language.code }
    suspend fun setApiKeyOverride(value: String) = edit { it[Keys.apiKey] = value.trim() }
    suspend fun setBaseUrlOverride(value: String) = edit { it[Keys.baseUrl] = value.trim() }
    suspend fun setModelOverride(value: String) = edit { it[Keys.model] = value.trim() }
    suspend fun setLlmProvider(provider: LlmProvider) = edit { it[Keys.llmProvider] = provider.name }
    suspend fun setLocalModelId(id: String) = edit { it[Keys.localModelId] = id }
    suspend fun setAnkiDeck(value: String) = edit { it[Keys.ankiDeck] = value.trim() }
    suspend fun setAnkiNoteType(type: AnkiNoteType) = edit { it[Keys.ankiNoteType] = type.name }
    suspend fun setAnkiIncludeSentence(value: Boolean) = edit { it[Keys.ankiIncludeSentence] = value }
    suspend fun setAnkiExportTarget(target: ExportTarget) = edit { it[Keys.ankiExportTarget] = target.name }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
