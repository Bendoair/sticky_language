package com.stickylanguage.app.ui.grammar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.anki.AnkiNoteSheet
import com.stickylanguage.app.ui.anki.WordSheet
import com.stickylanguage.app.ui.components.DashedRule
import com.stickylanguage.app.ui.components.ErrorBanner
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyButton
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickyIconButton
import com.stickylanguage.app.ui.components.StickyTextButton
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.app.ui.components.stickyContentInsets
import com.stickylanguage.app.ui.theme.sticky
import com.stickylanguage.shared.model.Chunk
import com.stickylanguage.shared.model.SentenceBreakdown

@Composable
fun GrammarScreen(
    onBack: () -> Unit,
    viewModel: GrammarViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chrome = LocalAppChrome.current
    LaunchedEffect(Unit) { viewModel.messages.collect { chrome.snackbar.showSnackbar(it) } }

    val c = MaterialTheme.sticky
    val s = ui.session
    val breakdown = s.breakdown?.takeIf { !s.breakdownIsStale }
    val sentences = breakdown?.sentences.orEmpty()

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "Grammar breakdown",
                onBack = onBack,
                actions = {
                    StickyIconButton(
                        Icons.Default.Refresh,
                        "Regenerate",
                        onClick = viewModel::regenerate,
                        enabled = !s.breakingDown && s.sourceText.isNotBlank(),
                    )
                },
            )
        },
        contentWindowInsets = stickyContentInsets(),
        containerColor = c.paper,
    ) { padding ->
        when {
            s.breakingDown -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = c.ink, trackColor = c.fill)
                    Spacer(Modifier.height(12.dp))
                    Text("Analysing sentences…", style = MaterialTheme.typography.bodyMedium, color = c.mid)
                }
            }

            s.breakdownError != null -> Box(Modifier.padding(padding).padding(16.dp)) {
                ErrorBanner(s.breakdownError, onRetry = viewModel::retry)
            }

            breakdown == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No breakdown yet.", color = c.mid)
            }

            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${sentences.size} sentence${if (sentences.size == 1) "" else "s"} · tap one to expand",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mid,
                            modifier = Modifier.weight(1f),
                        )
                        StickyTextButton(
                            text = if (ui.expanded.size == sentences.size) "Collapse all" else "Expand all",
                            onClick = {
                                if (ui.expanded.size == sentences.size) viewModel.collapseAll() else viewModel.expandAll(sentences.size)
                            },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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

/** Pastel chunk colours (always light; text drawn in fixed dark ink so they read the same in dark mode). */
private val chunkPalette = listOf(
    0xFFFFE28A, 0xFFBFE3B4, 0xFFBBD7F2, 0xFFF6C0C0, 0xFFD9C8F0, 0xFFF9D5A7, 0xFFC9ECEA,
)
private val chunkInk = Color(0xFF222222)
private val chunkMid = Color(0xFF5B564C)

/** Prototype `.sent`: 2dp border (3dp when open); open head turns yellow with a dashed rule. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceCard(
    index: Int,
    sentence: SentenceBreakdown,
    expanded: Boolean,
    selectedChunk: Int?,
    onToggle: () -> Unit,
    onChunkTap: (Int) -> Unit,
    onLookup: (Chunk) -> Unit,
) {
    val c = MaterialTheme.sticky
    StickyCard(
        borderWidth = if (expanded) 3.dp else 2.dp,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (expanded) c.highlight else c.paper)
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val headFg = if (expanded) c.onHighlight else c.ink
                Box(
                    Modifier
                        .size(26.dp)
                        .border(2.dp, headFg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = headFg)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sentence.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = headFg)
                    Text(
                        sentence.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (expanded) headFg.copy(alpha = 0.75f) else c.mid,
                    )
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = headFg)
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    DashedRule(color = c.ink)
                    Column(Modifier.padding(14.dp)) {
                        // Colour-coded chunk strip
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sentence.chunks.forEachIndexed { ci, chunk ->
                                val color = Color(chunkPalette[ci % chunkPalette.size])
                                val selected = selectedChunk == ci
                                Column(
                                    Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(color.copy(alpha = if (selected) 1f else 0.6f))
                                        .border(if (selected) 2.dp else 1.dp, if (selected) chunkInk else chunkMid.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                                        .clickable { onChunkTap(ci) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(chunk.text, style = MaterialTheme.typography.bodyLarge, color = chunkInk)
                                    Text(chunk.role, style = MaterialTheme.typography.labelSmall, color = chunkMid)
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
                                            .background(Color(chunkPalette[ci % chunkPalette.size]), CircleShape)
                                            .border(1.dp, c.ink, CircleShape),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(chunk.text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(chunk.role, style = MaterialTheme.typography.labelMedium, color = c.mid)
                                }
                                if (chunk.morph.isNotBlank()) {
                                    Text(chunk.morph, style = MaterialTheme.typography.bodySmall, color = c.mid, modifier = Modifier.padding(start = 18.dp))
                                }
                                AnimatedVisibility(visible = selected) {
                                    Column(Modifier.padding(start = 18.dp, top = 6.dp)) {
                                        if (chunk.explanation.isNotBlank()) {
                                            Text(chunk.explanation, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StickyButton(onClick = { onLookup(chunk) }, compact = true) {
                                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Look up")
                                            }
                                            StickyButton(onClick = { onLookup(chunk) }, compact = true, primary = true) {
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
                                StickyCard(
                                    muted = true,
                                    dashed = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                ) {
                                    Column {
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
}
