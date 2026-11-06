package com.stickylanguage.app.ui.anki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyButton
import com.stickylanguage.app.ui.components.StickyTextButton
import com.stickylanguage.app.ui.components.stickySwitchColors
import com.stickylanguage.app.ui.components.stickyTextFieldColors
import com.stickylanguage.app.ui.theme.sticky
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
    val c = MaterialTheme.sticky

    var word by remember(draft) { mutableStateOf(draft.word) }
    var reading by remember(draft) { mutableStateOf(draft.reading) }
    var meaning by remember(draft) { mutableStateOf(draft.meaning) }
    var grammar by remember(draft) { mutableStateOf(draft.grammar) }
    var sentence by remember(draft) { mutableStateOf(draft.sentence) }
    var deck by remember(draft) { mutableStateOf(draft.deck(defaults)) }
    var noteType by remember(draft) { mutableStateOf(draft.noteType(defaults)) }
    var includeSentence by remember(draft) { mutableStateOf(draft.includeSentence(defaults)) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    val fieldColors = stickyTextFieldColors()
    val fieldShape = MaterialTheme.shapes.small

    StickySheet(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Anki card", style = MaterialTheme.typography.headlineSmall)

            SectionLabel("Card content")
            OutlinedTextField(value = word, onValueChange = { word = it }, label = { Text("Word") }, singleLine = true, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reading, onValueChange = { reading = it }, label = { Text("Reading") }, singleLine = true, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("Meaning") }, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = grammar, onValueChange = { grammar = it }, label = { Text("Grammar note") }, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())

            SectionLabel("Anki settings")
            OutlinedTextField(value = deck, onValueChange = { deck = it }, label = { Text("Deck") }, singleLine = true, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                OutlinedTextField(
                    value = noteType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Note type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    colors = fieldColors,
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = typeMenuOpen,
                    onDismissRequest = { typeMenuOpen = false },
                    containerColor = c.paper,
                ) {
                    AnkiNoteType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label, color = c.ink) }, onClick = { noteType = t; typeMenuOpen = false })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Include source sentence", style = MaterialTheme.typography.bodyLarge)
                    if (sentence.isNotBlank()) {
                        Text(sentence, style = MaterialTheme.typography.bodySmall, color = c.mid, maxLines = 2)
                    }
                }
                Switch(checked = includeSentence, onCheckedChange = { includeSentence = it }, colors = stickySwitchColors())
            }
            if (includeSentence) {
                OutlinedTextField(value = sentence, onValueChange = { sentence = it }, label = { Text("Sentence") }, colors = fieldColors, shape = fieldShape, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))
            StickyButton(
                primary = true,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    StickyTextButton("Remove from export", onClick = onDelete)
                }
            }
        }
    }
}
