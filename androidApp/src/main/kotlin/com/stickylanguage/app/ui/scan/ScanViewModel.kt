package com.stickylanguage.app.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.stickylanguage.app.data.session.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(private val session: SessionController) : ViewModel() {
    /** Shutter pressed or gallery image chosen: OCR then one translate call. */
    fun onImage(uri: Uri) = session.startFromImage(uri)
}
