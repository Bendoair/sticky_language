package com.stickylanguage.app.ui.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.anki.AnkiExporter
import com.stickylanguage.app.anki.ExportOutcome
import com.stickylanguage.app.anki.ExportPlan
import com.stickylanguage.app.data.export.ExportRepository
import com.stickylanguage.app.data.settings.AppSettings
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.shared.anki.ExportNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot UI effects the screen must perform (launch SAF picker / permission request). */
sealed interface ExportEffect {
    data class CreateFile(val suggestedName: String) : ExportEffect
    data object RequestAnkiDroidPermission : ExportEffect
    data class Message(val text: String) : ExportEffect
}

data class ExportUi(
    val notes: List<ExportNote> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val editing: ExportNote? = null,
    val exporting: Boolean = false,
    val ankiDroidInstalled: Boolean = false,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repo: ExportRepository,
    settingsRepo: SettingsRepository,
    private val exporter: AnkiExporter,
) : ViewModel() {

    private data class Local(val editing: ExportNote? = null, val exporting: Boolean = false)

    private val local = MutableStateFlow(Local())
    private val _effects = Channel<ExportEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val ui: StateFlow<ExportUi> = combine(repo.notes, settingsRepo.settings, local) { notes, settings, l ->
        ExportUi(
            notes = notes,
            settings = settings,
            editing = l.editing,
            exporting = l.exporting,
            ankiDroidInstalled = exporter.ankiDroidInstalled(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUi())

    fun edit(note: ExportNote) = local.update { it.copy(editing = note) }
    fun dismissEdit() = local.update { it.copy(editing = null) }

    fun saveEdit(note: ExportNote) {
        viewModelScope.launch {
            repo.update(note)
            local.update { it.copy(editing = null) }
        }
    }

    fun remove(note: ExportNote) {
        viewModelScope.launch {
            repo.remove(note)
            local.update { it.copy(editing = null) }
        }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clear() }
    }

    /** "Export all" tap: decide between AnkiDroid and a file, asking for what is needed. */
    fun exportAll() {
        viewModelScope.launch {
            when (val plan = exporter.plan()) {
                ExportPlan.Nothing -> _effects.send(ExportEffect.Message("Nothing to export"))
                ExportPlan.NeedsAnkiDroidPermission -> _effects.send(ExportEffect.RequestAnkiDroidPermission)
                ExportPlan.AnkiDroid -> runAnkiDroid()
                is ExportPlan.ApkgFile -> _effects.send(ExportEffect.CreateFile(plan.suggestedName))
            }
        }
    }

    fun onAnkiDroidPermission(granted: Boolean) {
        viewModelScope.launch {
            if (granted) runAnkiDroid()
            else _effects.send(ExportEffect.Message("AnkiDroid permission denied. Switch export place to .apkg in Settings."))
        }
    }

    fun onFileChosen(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            local.update { it.copy(exporting = true) }
            report(exporter.exportToFile(uri))
            local.update { it.copy(exporting = false) }
        }
    }

    private suspend fun runAnkiDroid() {
        local.update { it.copy(exporting = true) }
        report(exporter.exportToAnkiDroid())
        local.update { it.copy(exporting = false) }
    }

    private suspend fun report(outcome: ExportOutcome) {
        val text = when (outcome) {
            is ExportOutcome.Success -> "Exported ${outcome.count} card${if (outcome.count == 1) "" else "s"} to ${outcome.target}"
            is ExportOutcome.Failure -> "Export failed: ${outcome.message}"
        }
        _effects.send(ExportEffect.Message(text))
    }
}
