package com.stickylanguage.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.llm.local.LocalEnginePhase
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.anki.AnkiNoteSheet
import com.stickylanguage.app.ui.anki.WordSheet
import com.stickylanguage.app.ui.components.ErrorBanner
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyBottomBar
import com.stickylanguage.app.ui.components.StickyButton
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickySegmented
import com.stickylanguage.app.ui.components.StickyTextButton
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.app.ui.components.stickyContentInsets
import com.stickylanguage.app.ui.theme.sticky
import com.stickylanguage.shared.model.Token
import com.stickylanguage.shared.model.TranslationResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onGrammar: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chrome = LocalAppChrome.current
    LaunchedEffect(Unit) { viewModel.messages.collect { chrome.snackbar.showSnackbar(it) } }

    val c = MaterialTheme.sticky
    val s = ui.session
    val translation = s.translation
    val hasFreshTranslation = translation != null && !s.translationIsStale
    val highlighting = ui.mode == ReaderMode.Highlight
    // Tokens only line up with the text they were produced from.
    val tappableTokens = if (hasFreshTranslation) translation!!.tokens else emptyList()

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "Reader",
                onBack = onBack,
                actions = {
                    if (highlighting && tappableTokens.isNotEmpty()) {
                        StickyTextButton(
                            text = if (ui.multiSelect) "Done" else "Select",
                            onClick = { viewModel.setMultiSelect(!ui.multiSelect) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            StickyBottomBar {
                Column {
                    AnimatedVisibility(visible = ui.multiSelect) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp),
                        ) {
                            StickyTextButton(
                                text = if (ui.selected.isEmpty()) "Select all" else "Clear",
                                onClick = { if (ui.selected.isEmpty()) viewModel.selectAll() else viewModel.clearSelection() },
                            )
                            Spacer(Modifier.weight(1f))
                            StickyButton(primary = true, enabled = ui.selected.isNotEmpty() && !ui.exporting, onClick = viewModel::exportSelected, compact = true) {
                                Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (ui.exporting) "Loading words…" else "Export ${ui.selected.size} to Anki")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StickyButton(
                            onClick = viewModel::translate,
                            enabled = s.canTranslate,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (s.translating) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.ink)
                            } else {
                                Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (translation == null) "Translate" else "Translate again")
                        }
                        StickyButton(
                            primary = true,
                            onClick = onGrammar,
                            enabled = s.sourceText.isNotBlank() && !s.ocrInProgress,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Grammar", maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        contentWindowInsets = stickyContentInsets(),
        containerColor = c.paper,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            // ---- Source box: label + Highlight / Edit toggle ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionLabel("${ui.settings.learned.englishName} · source", Modifier.weight(1f))
                if (s.ocrInProgress) {
                    Text("Recognising…", style = MaterialTheme.typography.labelMedium, color = c.mid)
                    Spacer(Modifier.width(8.dp))
                }
                StickySegmented(
                    options = listOf("Highlight", "Edit"),
                    selectedIndex = if (highlighting) 0 else 1,
                    onSelect = { viewModel.setMode(if (it == 0) ReaderMode.Highlight else ReaderMode.Edit) },
                    small = true,
                    modifier = Modifier.width(150.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            if (highlighting) {
                HighlightSource(
                    text = s.sourceText,
                    tokens = tappableTokens,
                    selected = ui.selected,
                    multiSelect = ui.multiSelect,
                    ocrInProgress = s.ocrInProgress,
                    onTokenTap = viewModel::onTokenTap,
                    onEmptyTap = { viewModel.setMode(ReaderMode.Edit) },
                )
            } else {
                EditSource(
                    text = s.sourceText,
                    enabled = !s.ocrInProgress,
                    placeholder = if (s.ocrInProgress) "Reading text from the image…" else "Type or paste text, then press Translate",
                    onChange = viewModel::onSourceChange,
                )
            }
            if (s.ocrInProgress) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp), color = c.ink, trackColor = c.fill)
            }
            s.ocrError?.let {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(it, onDismiss = viewModel::clearErrors)
            }
            if (highlighting && tappableTokens.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ui.multiSelect) "Tap words to select them" else "Tap a word to look it up",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.mid,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---- Translation ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionLabel("${ui.settings.explanation.englishName} · translation", Modifier.weight(1f))
                if (s.translationIsStale) {
                    Text("Edited · translate again", style = MaterialTheme.typography.labelMedium, color = c.danger)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Only on-device translations are partial: their sentences show up as they arrive.
            val partial = translation?.takeIf { !it.isComplete }
            // The sentences that arrived stay visible under the error, and Retry fills in the rest.
            if (partial != null && s.translateError != null) {
                ErrorBanner(s.translateError, onRetry = viewModel::retryTranslation, onDismiss = viewModel::clearErrors)
                Spacer(Modifier.height(8.dp))
            }

            when {
                partial != null -> StickyCard(muted = !hasFreshTranslation, modifier = Modifier.fillMaxWidth()) {
                    PartialTranslationText(partial, translating = s.translating, color = if (hasFreshTranslation) c.ink else c.mid)
                }

                s.translating -> StickyCard(muted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.ink)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            when (s.localPhase) {
                                LocalEnginePhase.LoadingModel -> "Loading on-device model…"
                                LocalEnginePhase.Generating -> "Generating on-device…"
                                else -> "Translating…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                s.translateError != null -> ErrorBanner(s.translateError, onRetry = viewModel::retryTranslation, onDismiss = viewModel::clearErrors)

                translation == null -> StickyCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (s.ocrInProgress) "Translation will start when you press Translate after the text is recognised."
                        else "Press Translate to get a context-aware translation and tappable words.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.mid,
                    )
                }

                else -> StickyCard(muted = !hasFreshTranslation, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        translation.translation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasFreshTranslation) c.ink else c.mid,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (val sheet = ui.sheet) {
        is ReaderSheet.Word -> ui.wordLookup(sheet.tokenIndex)?.let { lookup ->
            WordSheet(
                lookup = lookup,
                onDismiss = viewModel::dismissSheet,
                onAddToAnki = viewModel::openAnkiAdd,
                loading = s.lookingUpWord == sheet.tokenIndex,
                error = s.wordLookupError,
                onRetry = { viewModel.retryWord(sheet.tokenIndex) },
            )
        }
        is ReaderSheet.AnkiAdd -> AnkiNoteSheet(
            draft = sheet.draft,
            defaults = ui.settings.anki,
            confirmLabel = "Add to export",
            onDismiss = viewModel::dismissSheet,
            onConfirm = viewModel::confirmAnkiAdd,
        )
        null -> Unit
    }
}

/** Edit mode: plain bordered text box (prototype `textarea`). */
@Composable
private fun EditSource(
    text: String,
    enabled: Boolean,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    val c = MaterialTheme.sticky
    StickyCard(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        BasicTextField(
            value = text,
            onValueChange = onChange,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.mid)
                }
                inner()
            },
        )
    }
}

private const val ARRIVING_DOTS = "arrivingDots"

/**
 * A translation that is still arriving: the sentences translated so far, followed by moving dots
 * while [translating]. On-device sentences arrive in order, so the finished ones are always the first.
 */
@Composable
private fun PartialTranslationText(translation: TranslationResult, translating: Boolean, color: Color) {
    val text = buildAnnotatedString {
        append(translation.translation)
        if (translating) {
            if (translation.translation.isNotEmpty()) append(" ")
            appendInlineContent(ARRIVING_DOTS, "…")
        }
    }
    val inlineContent = mapOf(
        ARRIVING_DOTS to InlineTextContent(Placeholder(1.5.em, 1.em, PlaceholderVerticalAlign.TextCenter)) {
            ArrivingDots(color, Modifier.fillMaxSize())
        },
    )
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color, inlineContent = inlineContent)
}

/** Three dots that light up one after another, looping. */
@Composable
private fun ArrivingDots(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "arrivingDots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "arrivingDotsPhase",
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(4.dp)
                    .alpha(if (phase.toInt() == i) 1f else 0.3f)
                    .background(color, CircleShape),
            )
        }
    }
}

/**
 * Highlight mode: read-only. With fresh tokens each word is an inline tappable span
 * (prototype `.tok`: dotted underline; selected = yellow). Otherwise the raw text is shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightSource(
    text: String,
    tokens: List<Token>,
    selected: Set<Int>,
    multiSelect: Boolean,
    ocrInProgress: Boolean,
    onTokenTap: (Int) -> Unit,
    onEmptyTap: () -> Unit,
) {
    val c = MaterialTheme.sticky
    StickyCard(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
        when {
            tokens.isNotEmpty() -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tokens.forEachIndexed { i, token ->
                    TokenSpan(
                        token = token,
                        selected = i in selected,
                        showMeaning = !multiSelect,
                        onClick = { onTokenTap(i) },
                    )
                }
            }

            text.isNotBlank() -> Column {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Press Translate to make the words tappable.",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.mid,
                )
            }

            else -> Text(
                if (ocrInProgress) "Reading text from the image…" else "Nothing here yet · tap to type",
                style = MaterialTheme.typography.bodyLarge,
                color = c.mid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .clickable(enabled = !ocrInProgress, onClick = onEmptyTap),
            )
        }
    }
}

/** One tappable word drawn inline (prototype `.tok` / `.tok.sel`). */
@Composable
private fun TokenSpan(token: Token, selected: Boolean, showMeaning: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.sticky
    val underline = if (selected) c.ink else c.mid
    Column(
        Modifier
            .background(if (selected) c.highlight else c.paper, MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            token.surface,
            style = MaterialTheme.typography.bodyLarge,
            color = c.ink,
            modifier = Modifier.drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = underline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = if (selected) null else PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                )
            },
        )
        if (showMeaning && token.primaryMeaning.isNotBlank()) {
            Text(
                token.primaryMeaning,
                style = MaterialTheme.typography.labelSmall,
                color = c.mid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
