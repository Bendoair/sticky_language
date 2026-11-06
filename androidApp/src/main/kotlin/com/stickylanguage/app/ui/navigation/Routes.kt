package com.stickylanguage.app.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose routes. */
object Routes {
    @Serializable data object Home
    @Serializable data object Settings
    @Serializable data object Export
    @Serializable data object Scan
    @Serializable data object Reader
    @Serializable data object Grammar
}
