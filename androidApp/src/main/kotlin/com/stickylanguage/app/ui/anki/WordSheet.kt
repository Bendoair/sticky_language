package com.stickylanguage.app.ui.anki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.stickylanguage.app.ui.components.ErrorBanner
import com.stickylanguage.app.ui.components.InkRule
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyButton
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickyTag
import com.stickylanguage.app.ui.theme.sticky

/** Paper sheet with a 2dp ink top rule and a small ink grab handle (prototype `.popup`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickySheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val c = MaterialTheme.sticky
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.paper,
        contentColor = c.ink,
        scrimColor = c.ink.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.large,
        dragHandle = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .background(c.ink, MaterialTheme.shapes.extraLarge),
                )
                InkRule()
            }
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .imePadding()
                .padding(bottom = 20.dp),
        ) { content() }
    }
}

/**
 * Yomitan-style pop-up for one word: meaning, reading, grammar, example, and the Add to Anki action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSheet(
    lookup: WordLookup,
    onDismiss: () -> Unit,
    onAddToAnki: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val c = MaterialTheme.sticky
    StickySheet(onDismiss = onDismiss) {
        Text(lookup.surface, style = MaterialTheme.typography.displaySmall)
        val sub = listOf(
            lookup.lemma.takeIf { it.isNotBlank() && it != lookup.surface }?.let { "→ $it" },
            lookup.reading.takeIf { it.isNotBlank() },
        ).filterNotNull().joinToString("  ·  ")
        if (sub.isNotBlank()) {
            Text(sub, style = MaterialTheme.typography.titleMedium, color = c.mid)
        }
        if (lookup.pos.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lookup.pos.split(",", "+", "·").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                    StickyTag(tag)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Meaning")
        Spacer(Modifier.height(4.dp))
        when {
            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.ink)
                Spacer(Modifier.width(12.dp))
                Text("Looking up…", style = MaterialTheme.typography.bodyMedium)
            }

            error != null -> ErrorBanner(error, onRetry = onRetry)

            lookup.meanings.isEmpty() -> Text("—", style = MaterialTheme.typography.bodyLarge)

            else -> lookup.meanings.forEachIndexed { i, m ->
                Text("${i + 1}. $m", style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (lookup.grammar.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Grammar")
            Spacer(Modifier.height(4.dp))
            Text(lookup.grammar, style = MaterialTheme.typography.bodyMedium)
        }

        if (lookup.example.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Example")
            Spacer(Modifier.height(4.dp))
            StickyCard(muted = true, dashed = true, modifier = Modifier.fillMaxWidth()) {
                Text(lookup.example, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
        }

        if (lookup.sentence.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("In this text")
            Spacer(Modifier.height(4.dp))
            Text(lookup.sentence, style = MaterialTheme.typography.bodyMedium)
            if (lookup.sentenceTranslation.isNotBlank()) {
                Text(lookup.sentenceTranslation, style = MaterialTheme.typography.bodySmall, color = c.mid)
            }
        }

        Spacer(Modifier.height(24.dp))
        StickyButton(primary = true, enabled = !loading && error == null, onClick = onAddToAnki, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AddCard, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add to Anki")
        }
    }
}
