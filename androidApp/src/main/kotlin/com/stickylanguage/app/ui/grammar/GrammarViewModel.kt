package com.stickylanguage.app.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.data.export.ExportRepository
import com.stickylanguage.app.data.session.SessionController
import com.stickylanguage.app.data.session.SessionState
import com.stickylanguage.app.data.settings.AppSettings
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.app.ui.anki.WordLookup
import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.model.Chunk
import com.stickylanguage.shared.model.SentenceBreakdown
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

sealed interface GrammarSheet {
    data class Word(val lookup: WordLookup) : GrammarSheet
    data class AnkiAdd(val draft: ExportNote) : GrammarSheet
}

data class GrammarUi(
    val session: SessionState = SessionState(),
    val settings: AppSettings = AppSettings(),
    val expanded: Set<Int> = setOf(0),
    val selectedChunk: Map<Int, Int> = emptyMap(),
    val sheet: GrammarSheet? = null,
)

@HiltViewModel
class GrammarViewModel @Inject constructor(
    private val session: SessionController,
    settingsRepo: SettingsRepository,
    private val exportRepo: ExportRepository,
) : ViewModel() {

    private data class Local(
        val expanded: Set<Int> = setOf(0),
        val selectedChunk: Map<Int, Int> = emptyMap(),
        val sheet: GrammarSheet? = null,
    )

    private val local = MutableStateFlow(Local())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val ui: StateFlow<GrammarUi> = combine(session.state, settingsRepo.settings, local) { s, settings, l ->
        GrammarUi(session = s, settings = settings, expanded = l.expanded, selectedChunk = l.selectedChunk, sheet = l.sheet)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrammarUi())

    init {
        // Opening this screen is the explicit "Grammar breakdown" action: fetch once if needed.
        session.requestBreakdown()
    }

    fun regenerate() = session.requestBreakdown(force = true)
    fun retry() = session.requestBreakdown()

    fun toggleSentence(index: Int) = local.update { l ->
        l.copy(expanded = if (index in l.expanded) l.expanded - index else l.expanded + index)
    }

    fun expandAll(count: Int) = local.update { it.copy(expanded = (0 until count).toSet()) }
    fun collapseAll() = local.update { it.copy(expanded = emptySet()) }

    fun selectChunk(sentence: Int, chunk: Int) = local.update { l ->
        val cur = l.selectedChunk[sentence]
        l.copy(selectedChunk = if (cur == chunk) l.selectedChunk - sentence else l.selectedChunk + (sentence to chunk))
    }

    fun lookupChunk(sentence: SentenceBreakdown, chunk: Chunk) {
        val lookup = WordLookup.fromChunk(chunk, sentence.text, sentence.translation, ui.value.settings.learned.code)
        local.update { it.copy(sheet = GrammarSheet.Word(lookup)) }
    }

    fun openAnkiAdd() {
        val word = (local.value.sheet as? GrammarSheet.Word) ?: return
        local.update { it.copy(sheet = GrammarSheet.AnkiAdd(word.lookup.toExportNote())) }
    }

    fun dismissSheet() = local.update { it.copy(sheet = null) }

    fun confirmAnkiAdd(note: ExportNote) {
        viewModelScope.launch {
            exportRepo.add(note)
            local.update { it.copy(sheet = null) }
            _messages.send("“${note.word}” added to export")
        }
    }
}
