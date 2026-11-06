package com.stickylanguage.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickylanguage.app.data.export.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(exportRepository: ExportRepository) : ViewModel() {
    val exportCount: StateFlow<Int> =
        exportRepository.count.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
