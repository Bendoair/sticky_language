package com.stickylanguage.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickylanguage.app.data.settings.ThemeMode

// Sticky-note palette: warm paper, marker-yellow accent, ink text.
private val Ink = Color(0xFF1F1D1A)
private val Paper = Color(0xFFFFFCF2)
private val PaperDim = Color(0xFFF4EFDD)
private val Yellow = Color(0xFFFFD94A)
private val YellowDeep = Color(0xFF8A6D00)
private val YellowSoft = Color(0xFFFFF1B8)
private val Moss = Color(0xFF4E6B3A)
private val MossSoft = Color(0xFFDDE9CF)
private val Rust = Color(0xFFB4452F)

private val LightColors = lightColorScheme(
    primary = YellowDeep,
    onPrimary = Color.White,
    primaryContainer = Yellow,
    onPrimaryContainer = Ink,
    secondary = Moss,
    onSecondary = Color.White,
    secondaryContainer = MossSoft,
    onSecondaryContainer = Ink,
    tertiary = Rust,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = Color(0xFF5B564C),
    outline = Color(0xFFB8B1A0),
    error = Rust,
)

private val DarkColors = darkColorScheme(
    primary = Yellow,
    onPrimary = Ink,
    primaryContainer = Color(0xFF5C4A00),
    onPrimaryContainer = YellowSoft,
    secondary = Color(0xFFB7CFA3),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF384F29),
    onSecondaryContainer = MossSoft,
    tertiary = Color(0xFFFFB4A3),
    background = Color(0xFF151412),
    onBackground = Color(0xFFEDE7DA),
    surface = Color(0xFF151412),
    onSurface = Color(0xFFEDE7DA),
    surfaceVariant = Color(0xFF2A2721),
    onSurfaceVariant = Color(0xFFCEC6B4),
    outline = Color(0xFF8A8475),
)

val StickyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val StickyTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
    )
}

@Composable
fun StickyTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = StickyTypography,
        shapes = StickyShapes,
        content = content,
    )
}
