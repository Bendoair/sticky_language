package com.stickylanguage.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.anki.AnkiDroidApi
import com.stickylanguage.app.data.settings.AppSettings
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.data.settings.ThemeMode
import com.stickylanguage.app.llm.local.LocalLlmBackend
import com.stickylanguage.app.llm.local.ModelCatalog
import com.stickylanguage.app.llm.local.ModelDownloadState
import com.stickylanguage.app.llm.local.ModelDownloader
import com.stickylanguage.app.llm.local.ModelSpec
import com.stickylanguage.app.llm.local.ModelStore
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportTarget
import com.stickylanguage.shared.llm.LlmProvider
import com.stickylanguage.shared.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalModelRow(
    val spec: ModelSpec,
    val downloaded: Boolean,
    val selected: Boolean,
    val ramOk: Boolean,
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val localModels: List<LocalModelRow> = emptyList(),
    val download: ModelDownloadState = ModelDownloadState.Idle,
    val deviceRamMb: Int = 0,
    val freeStorageLabel: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val ankiDroid: AnkiDroidApi,
    private val store: ModelStore,
    private val downloader: ModelDownloader,
    private val localBackend: LocalLlmBackend,
) : ViewModel() {

    val ui: StateFlow<SettingsUiState> = combine(repo.settings, downloader.state) { settings, download ->
        SettingsUiState(
            settings = settings,
            localModels = ModelCatalog.all.map { spec ->
                LocalModelRow(
                    spec = spec,
                    downloaded = store.isReady(spec),
                    selected = settings.localModelId == spec.id,
                    ramOk = store.meetsRam(spec),
                )
            },
            download = download,
            deviceRamMb = store.deviceRamMb(),
            freeStorageLabel = formatBytes(store.freeBytes()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Convenience for screens that still want plain settings. */
    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val ankiDroidInstalled: Boolean get() = ankiDroid.isInstalled()

    fun setLearned(l: Language) = launch { repo.setLearned(l) }
    fun setExplanation(l: Language) = launch { repo.setExplanation(l) }
    fun setApiKey(v: String) = launch { repo.setApiKeyOverride(v) }
    fun setBaseUrl(v: String) = launch { repo.setBaseUrlOverride(v) }
    fun setModel(v: String) = launch { repo.setModelOverride(v) }
    fun setDeck(v: String) = launch { repo.setAnkiDeck(v) }
    fun setNoteType(t: AnkiNoteType) = launch { repo.setAnkiNoteType(t) }
    fun setIncludeSentence(v: Boolean) = launch { repo.setAnkiIncludeSentence(v) }
    fun setExportTarget(t: ExportTarget) = launch { repo.setAnkiExportTarget(t) }
    fun setTheme(m: ThemeMode) = launch { repo.setThemeMode(m) }

    fun setProvider(provider: LlmProvider) = launch {
        val previous = repo.current().llmProvider
        repo.setLlmProvider(provider)
        if (previous == LlmProvider.LOCAL && provider == LlmProvider.CLOUD) {
            localBackend.unload()
        }
    }

    fun selectLocalModel(id: String) = launch {
        val previous = repo.current().localModelId
        repo.setLocalModelId(id)
        if (previous != id && repo.current().llmProvider == LlmProvider.LOCAL) {
            localBackend.unload()
        }
    }

    fun downloadModel(spec: ModelSpec) = downloader.download(spec)
    fun cancelDownload() = downloader.cancel()
    fun deleteModel(spec: ModelSpec) = launch {
        val selected = repo.current().localModelId == spec.id
        downloader.delete(spec)
        if (selected) localBackend.unload()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "—"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format("%.1f GB free", gb) else String.format("%.0f MB free", bytes / (1024.0 * 1024.0))
    }
}
