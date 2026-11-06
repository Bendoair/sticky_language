package com.stickylanguage.app.ui.grammar

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.anki.AnkiNoteSheet
import com.stickylanguage.app.ui.anki.WordSheet
import com.stickylanguage.app.ui.components.ErrorBanner
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.shared.model.SentenceBreakdown

@Composable
fun GrammarScreen(
    onBack: () -> Unit,
    viewModel: GrammarViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chrome = LocalAppChrome.current
    LaunchedEffect(Unit) { viewModel.messages.collect { chrome.snackbar.showSnackbar(it) } }

    val s = ui.session
    val breakdown = s.breakdown?.takeIf { !s.breakdownIsStale }
    val sentences = breakdown?.sentences.orEmpty()

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "Grammar breakdown",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::regenerate, enabled = !s.breakingDown && s.sourceText.isNotBlank()) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            s.breakingDown -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Analysing sentences…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            s.breakdownError != null -> Box(Modifier.padding(padding).padding(20.dp)) {
                ErrorBanner(s.breakdownError, onRetry = viewModel::retry)
            }

            breakdown == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No breakdown yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${sentences.size} sentence${if (sentences.size == 1) "" else "s"} · tap one to expand",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            if (ui.expanded.size == sentences.size) viewModel.collapseAll() else viewModel.expandAll(sentences.size)
                        }) { Text(if (ui.expanded.size == sentences.size) "Collapse all" else "Expand all") }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                itemsIndexed(sentences) { index, sentence ->
                    SentenceCard(
                        index = index,
                        sentence = sentence,
                        expanded = index in ui.expanded,
                        selectedChunk = ui.selectedChunk[index],
                        onToggle = { viewModel.toggleSentence(index) },
                        onChunkTap = { viewModel.selectChunk(index, it) },
                        onLookup = { viewModel.lookupChunk(sentence, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    when (val sheet = ui.sheet) {
        is GrammarSheet.Word -> WordSheet(lookup = sheet.lookup, onDismiss = viewModel::dismissSheet, onAddToAnki = viewModel::openAnkiAdd)
        is GrammarSheet.AnkiAdd -> AnkiNoteSheet(
            draft = sheet.draft,
            defaults = ui.settings.anki,
            confirmLabel = "Add to export",
            onDismiss = viewModel::dismissSheet,
            onConfirm = viewModel::confirmAnkiAdd,
        )
        null -> Unit
    }
}

private val chunkPalette = listOf(
    0xFFFFE28A, 0xFFBFE3B4, 0xFFBBD7F2, 0xFFF6C0C0, 0xFFD9C8F0, 0xFFF9D5A7, 0xFFC9ECEA,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceCard(
    index: Int,
    sentence: SentenceBreakdown,
    expanded: Boolean,
    selectedChunk: Int?,
    onToggle: () -> Unit,
    onChunkTap: (Int) -> Unit,
    onLookup: (com.stickylanguage.shared.model.Chunk) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sentence.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(sentence.translation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // Colour-coded chunk strip
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sentence.chunks.forEachIndexed { ci, chunk ->
                            val color = androidx.compose.ui.graphics.Color(chunkPalette[ci % chunkPalette.size])
                            val selected = selectedChunk == ci
                            Column(
                                Modifier
                                    .background(color.copy(alpha = if (selected) 1f else 0.55f), MaterialTheme.shapes.extraSmall)
                                    .clickable { onChunkTap(ci) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(chunk.text, style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color(0xFF1F1D1A))
                                Text(chunk.role, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF5B564C))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    sentence.chunks.forEachIndexed { ci, chunk ->
                        val selected = selectedChunk == ci
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onChunkTap(ci) }
                                .padding(vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(androidx.compose.ui.graphics.Color(chunkPalette[ci % chunkPalette.size]), CircleShape),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(chunk.text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(chunk.role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (chunk.morph.isNotBlank()) {
                                Text(chunk.morph, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 18.dp))
                            }
                            AnimatedVisibility(visible = selected) {
                                Column(Modifier.padding(start = 18.dp, top = 4.dp)) {
                                    if (chunk.explanation.isNotBlank()) {
                                        Text(chunk.explanation, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row {
                                        TextButton(onClick = { onLookup(chunk) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Look up")
                                        }
                                        TextButton(onClick = { onLookup(chunk) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                                            Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Add to Anki")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (sentence.grammarPoints.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        SectionLabel("Grammar points")
                        Spacer(Modifier.height(6.dp))
                        sentence.grammarPoints.forEach { gp ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(gp.name, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(gp.body, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
