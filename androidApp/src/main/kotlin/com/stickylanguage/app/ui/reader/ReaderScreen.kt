package com.stickylanguage.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.anki.AnkiNoteSheet
import com.stickylanguage.app.ui.anki.WordSheet
import com.stickylanguage.app.ui.components.ErrorBanner
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.shared.model.Token

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

    val s = ui.session
    val translation = s.translation
    val hasFreshTranslation = translation != null && !s.translationIsStale

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "Reader",
                onBack = onBack,
                actions = {
                    if (translation != null) {
                        TextButton(onClick = { viewModel.setMultiSelect(!ui.multiSelect) }) {
                            Text(if (ui.multiSelect) "Done" else "Select")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    AnimatedVisibility(visible = ui.multiSelect) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp),
                        ) {
                            TextButton(onClick = { if (ui.selected.isEmpty()) viewModel.selectAll() else viewModel.clearSelection() }) {
                                Text(if (ui.selected.isEmpty()) "Select all" else "Clear")
                            }
                            Spacer(Modifier.weight(1f))
                            Button(enabled = ui.selected.isNotEmpty(), onClick = viewModel::exportSelected) {
                                Icon(Icons.Outlined.AddCard, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Export ${ui.selected.size} to Anki")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = viewModel::translate,
                            enabled = s.canTranslate,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (s.translating) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Translate, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (translation == null) "Translate" else "Translate again")
                        }
                        Button(
                            onClick = onGrammar,
                            enabled = s.sourceText.isNotBlank() && !s.ocrInProgress,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Grammar breakdown")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // ---- Source box ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionLabel("${ui.settings.learned.englishName} · source", Modifier.weight(1f))
                if (s.ocrInProgress) {
                    Text("Recognising…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = s.sourceText,
                onValueChange = viewModel::onSourceChange,
                enabled = !s.ocrInProgress,
                placeholder = { Text(if (s.ocrInProgress) "Reading text from the image…" else "Type or paste text, then press Translate") },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
            )
            if (s.ocrInProgress) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            s.ocrError?.let {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(it, onDismiss = viewModel::clearErrors)
            }

            Spacer(Modifier.height(20.dp))

            // ---- Translation ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionLabel("${ui.settings.explanation.englishName} · translation", Modifier.weight(1f))
                if (s.translationIsStale) {
                    Text("Edited · translate again", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Spacer(Modifier.height(6.dp))

            when {
                s.translating -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Translating…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                s.translateError != null -> ErrorBanner(s.translateError, onRetry = viewModel::translate, onDismiss = viewModel::clearErrors)

                translation == null -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (s.ocrInProgress) "Translation will start when the text is recognised."
                        else "Press Translate to get a context-aware translation and tappable words.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                else -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (hasFreshTranslation) 1f else 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            translation.translation,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    SectionLabel(if (ui.multiSelect) "Tap words to select" else "Words · tap to look up")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        translation.tokens.forEachIndexed { i, token ->
                            TokenChip(
                                token = token,
                                selected = i in ui.selected,
                                selectable = ui.multiSelect,
                                onClick = { viewModel.onTokenTap(i) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (val sheet = ui.sheet) {
        is ReaderSheet.Word -> WordSheet(lookup = sheet.lookup, onDismiss = viewModel::dismissSheet, onAddToAnki = viewModel::openAnkiAdd)
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

@Composable
fun TokenChip(token: Token, selected: Boolean, selectable: Boolean, onClick: () -> Unit) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        tonalElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), MaterialTheme.shapes.small),
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = if (selectable) 2.dp else 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
            if (selectable) {
                Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.size(32.dp))
            }
            Column {
                Text(token.surface, style = MaterialTheme.typography.titleMedium)
                if (token.primaryMeaning.isNotBlank()) {
                    Text(
                        token.primaryMeaning,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}