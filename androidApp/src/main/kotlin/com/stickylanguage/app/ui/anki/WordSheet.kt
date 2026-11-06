package com.stickylanguage.app.ui.anki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.stickylanguage.app.ui.components.SectionLabel

/**
 * Yomitan-style pop-up for one word: meaning, reading, grammar, example, and the Add to Anki action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordSheet(
    lookup: WordLookup,
    onDismiss: () -> Unit,
    onAddToAnki: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(lookup.surface, style = MaterialTheme.typography.displaySmall)
            val sub = listOf(
                lookup.lemma.takeIf { it.isNotBlank() && it != lookup.surface }?.let { "→ $it" },
                lookup.reading.takeIf { it.isNotBlank() },
            ).filterNotNull().joinToString("  ·  ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (lookup.pos.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    lookup.pos.split(",", "+", "·").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                        SuggestionChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Meaning")
            Spacer(Modifier.height(4.dp))
            if (lookup.meanings.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyLarge)
            } else {
                lookup.meanings.forEachIndexed { i, m ->
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
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(
                        lookup.example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (lookup.sentence.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("In this text")
                Spacer(Modifier.height(4.dp))
                Text(lookup.sentence, style = MaterialTheme.typography.bodyMedium)
                if (lookup.sentenceTranslation.isNotBlank()) {
                    Text(
                        lookup.sentenceTranslation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddToAnki, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddCard, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Add to Anki")
            }
        }
    }
}
