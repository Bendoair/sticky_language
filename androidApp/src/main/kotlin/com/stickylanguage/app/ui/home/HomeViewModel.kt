package com.stickylanguage.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.anki.AnkiDroidApi
import com.stickylanguage.app.data.export.ExportRepository
import com.stickylanguage.app.data.session.SessionController
import com.stickylanguage.app.data.settings.SettingsRepository
import com.stickylanguage.shared.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUi(
    val learned: Language = Language.KOREAN,
    val explanation: Language = Language.ENGLISH,
    val exportCount: Int = 0,
    val ankiDroidInstalled: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    settings: SettingsRepository,
    exportRepository: ExportRepository,
    ankiDroid: AnkiDroidApi,
    private val session: SessionController,
) : ViewModel() {

    val ui: StateFlow<HomeUi> = combine(settings.settings, exportRepository.count) { s, count ->
        HomeUi(
            learned = s.learned,
            explanation = s.explanation,
            exportCount = count,
            ankiDroidInstalled = ankiDroid.isInstalled(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    fun onImagePicked(uri: Uri) = session.startFromImage(uri)

    fun startTyping() = session.startEmpty()
}
