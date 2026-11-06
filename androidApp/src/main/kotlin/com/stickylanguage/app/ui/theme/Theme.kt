package com.stickylanguage.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickylanguage.app.R
import com.stickylanguage.app.data.settings.ThemeMode

/**
 * Lo-fi wireframe palette from reference/styles.css:
 * paper, ink, fill, mid, line, and one accent: sticky-note yellow.
 */
@Immutable
data class StickyColors(
    val paper: Color,
    val ink: Color,
    val fill: Color,
    val mid: Color,
    val line: Color,
    val highlight: Color,
    val onHighlight: Color,
    val danger: Color,
    val isDark: Boolean,
)

private val LightSticky = StickyColors(
    paper = Color(0xFFFFFFFF),
    ink = Color(0xFF222222),
    fill = Color(0xFFEEEEEE),
    mid = Color(0xFF777777),
    line = Color(0xFFBBBBBB),
    highlight = Color(0xFFFFF3A6),
    onHighlight = Color(0xFF222222),
    danger = Color(0xFFB4452F),
    isDark = false,
)

private val DarkSticky = StickyColors(
    paper = Color(0xFF161616),
    ink = Color(0xFFF2F2F2),
    fill = Color(0xFF2A2A2A),
    mid = Color(0xFF9A9A9A),
    line = Color(0xFF555555),
    highlight = Color(0xFFFFF3A6),
    onHighlight = Color(0xFF222222),
    danger = Color(0xFFFF9B86),
    isDark = true,
)

val LocalStickyColors = staticCompositionLocalOf { LightSticky }

val MaterialTheme.sticky: StickyColors
    @Composable @ReadOnlyComposable get() = LocalStickyColors.current

private fun StickyColors.toLightScheme() = lightColorScheme(
    primary = ink,
    onPrimary = paper,
    primaryContainer = highlight,
    onPrimaryContainer = onHighlight,
    secondary = mid,
    onSecondary = paper,
    secondaryContainer = fill,
    onSecondaryContainer = ink,
    tertiary = danger,
    onTertiary = paper,
    background = paper,
    onBackground = ink,
    surface = paper,
    onSurface = ink,
    surfaceVariant = fill,
    onSurfaceVariant = mid,
    surfaceContainer = paper,
    surfaceContainerLow = paper,
    surfaceContainerHigh = fill,
    surfaceContainerHighest = fill,
    outline = ink,
    outlineVariant = line,
    error = danger,
    onError = paper,
    errorContainer = Color(0xFFFBE5E0),
    onErrorContainer = danger,
)

private fun StickyColors.toDarkScheme() = darkColorScheme(
    primary = ink,
    onPrimary = paper,
    primaryContainer = highlight,
    onPrimaryContainer = onHighlight,
    secondary = mid,
    onSecondary = paper,
    secondaryContainer = fill,
    onSecondaryContainer = ink,
    tertiary = danger,
    onTertiary = paper,
    background = paper,
    onBackground = ink,
    surface = paper,
    onSurface = ink,
    surfaceVariant = fill,
    onSurfaceVariant = mid,
    surfaceContainer = paper,
    surfaceContainerLow = paper,
    surfaceContainerHigh = fill,
    surfaceContainerHighest = fill,
    outline = ink,
    outlineVariant = line,
    error = danger,
    onError = Color(0xFF3A0F08),
    errorContainer = Color(0xFF4A1A12),
    onErrorContainer = danger,
)

/** Square-ish corners like the wireframe (8dp cards, 10-12dp actions). */
val StickyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

/** Patrick Hand (OFL) bundled in res/font. Missing CJK glyphs fall back to system fonts. */
val HandFont = FontFamily(Font(R.font.patrick_hand_regular, FontWeight.Normal))

/**
 * Hand-written font for chrome (titles, buttons, labels); default sans for dense body text
 * so Korean / Japanese / Chinese passages stay readable.
 */
val StickyTypography: Typography = Typography().let { t ->
    fun TextStyle.hand(size: Int, weight: FontWeight = FontWeight.Normal) =
        copy(fontFamily = HandFont, fontSize = size.sp, fontWeight = weight, letterSpacing = 0.sp)

    t.copy(
        displaySmall = t.displaySmall.hand(34),
        headlineLarge = t.headlineLarge.hand(32),
        headlineMedium = t.headlineMedium.hand(28),
        headlineSmall = t.headlineSmall.hand(24),
        titleLarge = t.titleLarge.hand(22, FontWeight.Bold),
        titleMedium = t.titleMedium.hand(19),
        titleSmall = t.titleSmall.hand(17),
        labelLarge = t.labelLarge.hand(16),
        labelMedium = t.labelMedium.hand(14),
        labelSmall = t.labelSmall.hand(13),
        bodyLarge = t.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = t.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
    )
}

@Composable
fun StickyTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val sticky = if (dark) DarkSticky else LightSticky
    CompositionLocalProvider(LocalStickyColors provides sticky) {
        MaterialTheme(
            colorScheme = if (dark) sticky.toDarkScheme() else sticky.toLightScheme(),
            typography = StickyTypography,
            shapes = StickyShapes,
            content = content,
        )
    }
}
