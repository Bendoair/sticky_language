package com.stickylanguage.app

import android.app.Application
import android.content.ComponentCallbacks2
import com.stickylanguage.app.llm.local.LlamaCppEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StickyApp : Application() {

    @Inject lateinit var llamaEngine: LlamaCppEngine

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Only unload under real memory pressure — not every time the UI backgrounds.
        // TRIM_MEMORY_RUNNING_LOW == 10; still the right threshold on current SDKs.
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            if (::llamaEngine.isInitialized) {
                llamaEngine.unloadBlocking()
            }
        }
    }
}
