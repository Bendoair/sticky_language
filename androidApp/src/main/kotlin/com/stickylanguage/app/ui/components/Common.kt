package com.stickylanguage.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.theme.sticky

// ---------------------------------------------------------------------------------------------
// Lo-fi primitives: ink borders, paper fills, one yellow accent. See reference/styles.css.
// ---------------------------------------------------------------------------------------------

/**
 * Top bar with a 2dp ink rule underneath (prototype `.topbar`).
 * Shows a back button when [onBack] is set; top-level tabs have no leading icon
 * (navigation lives in the bottom tab bar).
 */
@Composable
fun StickyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = MaterialTheme.sticky
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                StickyIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        InkRule()
    }
}

/**
 * Screen content insets: when the root bottom tab bar is visible it already owns the
 * bottom safe-drawing inset, so child scaffolds only pad top + horizontal.
 */
@Composable
fun stickyContentInsets(): WindowInsets {
    val chrome = LocalAppChrome.current
    return if (chrome.showBottomNav) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing
    }
}

/** Bottom action area with a 2dp ink rule on top. Pads the system nav bar only when tabs are hidden. */
@Composable
fun StickyBottomBar(content: @Composable () -> Unit) {
    val c = MaterialTheme.sticky
    val chrome = LocalAppChrome.current
    val bottomSides = if (chrome.showBottomNav) {
        WindowInsetsSides.Horizontal
    } else {
        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.paper),
    ) {
        InkRule()
        Box(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(bottomSides))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) { content() }
    }
}

@Composable
fun InkRule(thickness: Dp = 2.dp, color: Color = MaterialTheme.sticky.ink) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

/** Dashed horizontal separator (prototype list rows). */
@Composable
fun DashedRule(color: Color = MaterialTheme.sticky.line, thickness: Dp = 1.5.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(thickness)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            },
    )
}

/** Circular icon button with a 2dp border in the current content colour (prototype `.icon-btn`). */
@Composable
fun StickyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 38.dp,
    tint: Color = LocalContentColor.current,
) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, tint.copy(alpha = alpha), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint.copy(alpha = alpha), modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Bordered card (prototype `.card`): 2dp ink border, 8dp radius, no elevation.
 * [dashed] draws the border dashed; [highlighted] fills with sticky yellow; [muted] fills with grey.
 */
@Composable
fun StickyCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    muted: Boolean = false,
    dashed: Boolean = false,
    borderWidth: Dp = 2.dp,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit,
) {
    val c = MaterialTheme.sticky
    val bg = when {
        highlighted -> c.highlight
        muted -> c.fill
        else -> c.paper
    }
    val fg = if (highlighted) c.onHighlight else c.ink
    val borderColor = when {
        dashed -> c.line
        highlighted -> c.onHighlight
        else -> c.ink
    }
    val base = modifier
        .clip(shape)
        .background(bg)
        .then(
            if (dashed) Modifier.dashedBorder(borderWidth, borderColor, shape)
            else Modifier.border(borderWidth, borderColor, shape),
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Box(base.padding(contentPadding)) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}

fun Modifier.dashedBorder(width: Dp, color: Color, shape: Shape): Modifier = drawBehind {
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
    drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color, style = stroke)
}

/**
 * Bordered button (prototype `.btn`): 2dp ink border, 8dp radius.
 * [primary] = ink fill / paper text (prototype `.btn.primary`).
 */
@Composable
fun StickyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val c = MaterialTheme.sticky
    val alpha = if (enabled) 1f else 0.4f
    val bg = if (primary) c.ink.copy(alpha = alpha) else c.paper
    val fg = if (primary) c.paper else c.ink.copy(alpha = alpha)
    val border = c.ink.copy(alpha = alpha)
    val shape = MaterialTheme.shapes.small
    Row(
        modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = fg)) { content() }
        }
    }
}

/** Text-only inline action (prototype `.tiny.link`). */
@Composable
fun StickyTextButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val c = MaterialTheme.sticky
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = c.ink.copy(alpha = if (enabled) 1f else 0.4f),
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Segmented control (prototype `.segmented`): bordered, selected segment ink-filled. */
@Composable
fun StickySegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val c = MaterialTheme.sticky
    val shape = MaterialTheme.shapes.small
    Row(
        modifier
            .clip(shape)
            .border(2.dp, c.ink, shape)
            .height(IntrinsicSize.Min)
            .heightIn(min = if (small) 32.dp else 40.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) c.ink else c.paper)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = if (small) 5.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = if (on) c.paper else c.ink,
                    maxLines = 1,
                )
            }
            if (i < options.lastIndex) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(c.ink),
                )
            }
        }
    }
}

/** Small bordered tag (prototype `.tags span`). */
@Composable
fun StickyTag(text: String, highlighted: Boolean = false) {
    val c = MaterialTheme.sticky
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = c.ink,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (highlighted) c.highlight else Color.Transparent)
            .border(1.5.dp, c.ink, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Uppercase grey label (prototype `.label`). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.sticky.mid,
        modifier = modifier,
    )
}

/** Simple list row with a dashed rule underneath (prototype `.list li`). */
@Composable
fun StickyListRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.sticky.mid)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        DashedRule()
    }
}

/** Ink-outlined text field colours; use with [androidx.compose.material3.OutlinedTextField]. */
@Composable
fun stickyTextFieldColors(): TextFieldColors {
    val c = MaterialTheme.sticky
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.ink,
        unfocusedBorderColor = c.ink,
        disabledBorderColor = c.line,
        focusedLabelColor = c.ink,
        unfocusedLabelColor = c.mid,
        cursorColor = c.ink,
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        focusedPlaceholderColor = c.mid,
        unfocusedPlaceholderColor = c.mid,
        focusedContainerColor = c.paper,
        unfocusedContainerColor = c.paper,
        focusedTrailingIconColor = c.ink,
        unfocusedTrailingIconColor = c.mid,
    )
}

/** Ink radio button colours. */
@Composable
fun stickyRadioColors(): RadioButtonColors {
    val c = MaterialTheme.sticky
    return RadioButtonDefaults.colors(selectedColor = c.ink, unselectedColor = c.mid)
}

/** Ink/yellow switch colours (prototype only has one accent). */
@Composable
fun stickySwitchColors(): SwitchColors {
    val c = MaterialTheme.sticky
    return SwitchDefaults.colors(
        checkedThumbColor = c.ink,
        checkedTrackColor = c.highlight,
        checkedBorderColor = c.ink,
        uncheckedThumbColor = c.mid,
        uncheckedTrackColor = c.paper,
        uncheckedBorderColor = c.mid,
    )
}

@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    val c = MaterialTheme.sticky
    StickyCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = c.danger)
            if (onRetry != null || onDismiss != null) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (onDismiss != null) StickyTextButton("Dismiss", onDismiss)
                    if (onRetry != null) StickyTextButton("Retry", onRetry)
                }
            }
        }
    }
}