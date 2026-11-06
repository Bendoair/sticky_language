package com.stickylanguage.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.data.export.ExportRepository
import com.stickylanguage.app.data.session.SessionController
import com.stickylanguage.app.data.session.SessionState
import com.stickylanguage.app.data.settings.AppSettings
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.ui.anki.WordLookup
import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.model.Token
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

sealed interface ReaderSheet {
    data class Word(val lookup: WordLookup) : ReaderSheet
    data class AnkiAdd(val draft: ExportNote) : ReaderSheet
}

data class ReaderUi(
    val session: SessionState = SessionState(),
    val settings: AppSettings = AppSettings(),
    val multiSelect: Boolean = false,
    val selected: Set<Int> = emptySet(),
    val sheet: ReaderSheet? = null,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val session: SessionController,
    private val settingsRepo: SettingsRepository,
    private val exportRepo: ExportRepository,
) : ViewModel() {

    private data class Local(val multiSelect: Boolean = false, val selected: Set<Int> = emptySet(), val sheet: ReaderSheet? = null)

    private val local = MutableStateFlow(Local())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val ui: StateFlow<ReaderUi> = combine(session.state, settingsRepo.settings, local) { s, settings, l ->
        ReaderUi(session = s, settings = settings, multiSelect = l.multiSelect, selected = l.selected, sheet = l.sheet)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUi())

    // ----- Source / LLM triggers (explicit only) -----

    fun onSourceChange(text: String) {
        session.updateSource(text)
        // Token indices no longer line up with edited text.
        local.update { it.copy(selected = emptySet()) }
    }

    fun translate() = session.translate()

    fun clearErrors() = session.clearErrors()

    // ----- Word pop-up -----

    fun onTokenTap(index: Int) {
        val st = ui.value
        if (st.multiSelect) {
            local.update { l -> l.copy(selected = if (index in l.selected) l.selected - index else l.selected + index) }
            return
        }
        val token = st.session.translation?.tokens?.getOrNull(index) ?: return
        val lookup = WordLookup.fromToken(token, st.session.translation, st.settings.learned.code)
        local.update { it.copy(sheet = ReaderSheet.Word(lookup)) }
    }

    fun openAnkiAdd() {
        val word = (local.value.sheet as? ReaderSheet.Word) ?: return
        local.update { it.copy(sheet = ReaderSheet.AnkiAdd(word.lookup.toExportNote())) }
    }

    fun dismissSheet() = local.update { it.copy(sheet = null) }

    fun confirmAnkiAdd(note: ExportNote) {
        viewModelScope.launch {
            exportRepo.add(note)
            local.update { it.copy(sheet = null) }
            _messages.send("“${note.word}” added to export")
        }
    }

    // ----- Multi-select -----

    fun setMultiSelect(enabled: Boolean) {
        local.update { it.copy(multiSelect = enabled, selected = if (enabled) it.selected else emptySet()) }
    }

    fun selectAll() {
        val n = ui.value.session.translation?.tokens?.size ?: 0
        local.update { it.copy(selected = (0 until n).toSet()) }
    }

    fun clearSelection() = local.update { it.copy(selected = emptySet()) }

    /** "Export to Anki" with default settings: pushes every selected token to the heap. */
    fun exportSelected() {
        val st = ui.value
        val tokens = st.session.translation?.tokens ?: return
        val notes = st.selected.sorted().mapNotNull { i -> tokens.getOrNull(i) }
            .map { t -> WordLookup.fromToken(t, st.session.translation, st.settings.learned.code).toExportNote() }
        if (notes.isEmpty()) return
        viewModelScope.launch {
            exportRepo.addAll(notes)
            local.update { it.copy(selected = emptySet(), multiSelect = false) }
            _messages.send("${notes.size} words added to export")
        }
    }

    fun tokenAt(index: Int): Token? = ui.value.session.translation?.tokens?.getOrNull(index)
}
