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
import kotlinx.coroutines.CancellationException
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
    /** Holds an index, not a snapshot, so the open card updates when its details arrive. */
    data class Word(val tokenIndex: Int) : ReaderSheet
    data class AnkiAdd(val draft: ExportNote) : ReaderSheet
}

/**
 * Highlight: source is read-only; words are tappable (look-up / multi-select).
 * Edit: source is a text field; no word taps, so you can't edit by accident while highlighting.
 */
enum class ReaderMode { Highlight, Edit }

data class ReaderUi(
    val session: SessionState = SessionState(),
    val settings: AppSettings = AppSettings(),
    val mode: ReaderMode = ReaderMode.Highlight,
    val multiSelect: Boolean = false,
    val selected: Set<Int> = emptySet(),
    val sheet: ReaderSheet? = null,
    val exporting: Boolean = false,
) {
    fun wordLookup(tokenIndex: Int): WordLookup? {
        val translation = session.translation ?: return null
        val token = translation.tokens.getOrNull(tokenIndex) ?: return null
        return WordLookup.fromToken(token, translation, settings.learned.code)
    }
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val session: SessionController,
    private val settingsRepo: SettingsRepository,
    private val exportRepo: ExportRepository,
) : ViewModel() {

    private data class Local(
        val mode: ReaderMode,
        val multiSelect: Boolean = false,
        val selected: Set<Int> = emptySet(),
        val sheet: ReaderSheet? = null,
        val exporting: Boolean = false,
    )

    // Default to Highlight; start in Edit only when there is nothing to highlight yet (Home → Type).
    private val local = MutableStateFlow(
        Local(
            mode = session.state.value.let { s ->
                if (s.sourceText.isBlank() && !s.ocrInProgress) ReaderMode.Edit else ReaderMode.Highlight
            },
        ),
    )
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val ui: StateFlow<ReaderUi> = combine(session.state, settingsRepo.settings, local) { s, settings, l ->
        ReaderUi(
            session = s,
            settings = settings,
            mode = l.mode,
            multiSelect = l.multiSelect,
            selected = l.selected,
            sheet = l.sheet,
            exporting = l.exporting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUi())

    // ----- Mode -----

    fun setMode(mode: ReaderMode) {
        local.update {
            // Leaving Highlight drops any pending multi-selection; token taps are impossible in Edit.
            if (mode == ReaderMode.Edit) it.copy(mode = mode, multiSelect = false, selected = emptySet())
            else it.copy(mode = mode)
        }
    }

    // ----- Source / LLM triggers (explicit only) -----

    fun onSourceChange(text: String) {
        session.updateSource(text)
        // Token indices no longer line up with edited text.
        local.update { it.copy(selected = emptySet()) }
    }

    /** Explicit user action. Switch back to Highlight so the new tokens are tappable as soon as they land. */
    fun translate() {
        session.translate()
        local.update { it.copy(mode = ReaderMode.Highlight) }
    }

    /** Retry after a failure: a partial on-device translation keeps its sentences and fills in the rest. */
    fun retryTranslation() {
        session.retryTranslation()
        local.update { it.copy(mode = ReaderMode.Highlight) }
    }

    fun clearErrors() = session.clearErrors()

    // ----- Word pop-up -----

    fun onTokenTap(index: Int) {
        val st = ui.value
        if (st.multiSelect) {
            local.update { l -> l.copy(selected = if (index in l.selected) l.selected - index else l.selected + index) }
            return
        }
        if (st.wordLookup(index) == null) return
        session.requestWord(index)
        local.update { it.copy(sheet = ReaderSheet.Word(index)) }
    }

    fun retryWord(index: Int) = session.requestWord(index)

    fun openAnkiAdd() {
        val word = (local.value.sheet as? ReaderSheet.Word) ?: return
        val lookup = ui.value.wordLookup(word.tokenIndex) ?: return
        local.update { it.copy(sheet = ReaderSheet.AnkiAdd(lookup.toExportNote())) }
    }

    fun dismissSheet() {
        session.cancelWordLookup()
        local.update { it.copy(sheet = null) }
    }

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

    /** "Export to Anki" with default settings: fetches missing details, then pushes every selected token to the heap. */
    fun exportSelected() {
        val st = ui.value
        val indices = st.selected.sorted()
        if (indices.isEmpty() || st.exporting) return
        viewModelScope.launch {
            local.update { it.copy(exporting = true) }
            try {
                session.fetchMissingWordDetails(indices)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send("Couldn't load word details: ${e.message}")
                return@launch
            } finally {
                local.update { it.copy(exporting = false) }
            }
            // The text may have been re-translated during the fetch, which invalidates the indices.
            val current = ui.value
            if (current.session.translatedSource != st.session.translatedSource) return@launch
            val notes = indices.mapNotNull { i -> current.wordLookup(i)?.toExportNote() }
            exportRepo.addAll(notes)
            local.update { it.copy(selected = emptySet(), multiSelect = false) }
            _messages.send("${notes.size} words added to export")
        }
    }

    fun tokenAt(index: Int): Token? = ui.value.session.translation?.tokens?.getOrNull(index)
}
