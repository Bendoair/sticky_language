package com.stickylanguage.app.ui.anki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.shared.anki.AnkiDefaults
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportNote

/**
 * Per-note Anki settings: used when adding a word (Reader/Grammar) and when editing a queued note (Export).
 * Only values that differ from [defaults] are stored as overrides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnkiNoteSheet(
    draft: ExportNote,
    defaults: AnkiDefaults,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (ExportNote) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var word by remember(draft) { mutableStateOf(draft.word) }
    var reading by remember(draft) { mutableStateOf(draft.reading) }
    var meaning by remember(draft) { mutableStateOf(draft.meaning) }
    var grammar by remember(draft) { mutableStateOf(draft.grammar) }
    var sentence by remember(draft) { mutableStateOf(draft.sentence) }
    var deck by remember(draft) { mutableStateOf(draft.deck(defaults)) }
    var noteType by remember(draft) { mutableStateOf(draft.noteType(defaults)) }
    var includeSentence by remember(draft) { mutableStateOf(draft.includeSentence(defaults)) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Anki card", style = MaterialTheme.typography.headlineSmall)

            SectionLabel("Card content")
            OutlinedTextField(value = word, onValueChange = { word = it }, label = { Text("Word") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reading, onValueChange = { reading = it }, label = { Text("Reading") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("Meaning") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = grammar, onValueChange = { grammar = it }, label = { Text("Grammar note") }, modifier = Modifier.fillMaxWidth())

            SectionLabel("Anki settings")
            OutlinedTextField(value = deck, onValueChange = { deck = it }, label = { Text("Deck") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                OutlinedTextField(
                    value = noteType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Note type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    AnkiNoteType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label) }, onClick = { noteType = t; typeMenuOpen = false })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Include source sentence", style = MaterialTheme.typography.bodyLarge)
                    if (sentence.isNotBlank()) {
                        Text(sentence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
                Switch(checked = includeSentence, onCheckedChange = { includeSentence = it })
            }
            if (includeSentence) {
                OutlinedTextField(value = sentence, onValueChange = { sentence = it }, label = { Text("Sentence") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))
            Button(
                enabled = word.isNotBlank() && meaning.isNotBlank(),
                onClick = {
                    onConfirm(
                        draft.copy(
                            word = word.trim(),
                            reading = reading.trim(),
                            meaning = meaning.trim(),
                            grammar = grammar.trim(),
                            sentence = sentence.trim(),
                            deckOverride = deck.trim().takeIf { it.isNotBlank() && it != defaults.deckName },
                            noteTypeOverride = noteType.takeIf { it != defaults.noteType },
                            includeSentenceOverride = includeSentence.takeIf { it != defaults.includeSentence },
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(confirmLabel) }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove from export", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
