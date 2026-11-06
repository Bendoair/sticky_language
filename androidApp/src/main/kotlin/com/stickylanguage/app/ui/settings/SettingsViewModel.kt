package com.stickylanguage.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.anki.AnkiDroidApi
import com.stickylanguage.app.data.settings.AppSettings
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.data.settings.ThemeMode
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportTarget
import com.stickylanguage.shared.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val ankiDroid: AnkiDroidApi,
) : ViewModel() {

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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
