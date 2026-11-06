package com.stickylanguage.app.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.anki.AnkiDroidApi
import com.stickylanguage.app.ui.LocalAppChrome
import com.stickylanguage.app.ui.anki.AnkiNoteSheet
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.anki.ExportTarget

@Composable
fun ExportScreen(viewModel: ExportViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chrome = LocalAppChrome.current
    var confirmClear by remember { mutableStateOf(false) }

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        viewModel.onFileChosen(uri)
    }
    val requestAnkiPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onAnkiDroidPermission(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.CreateFile -> createFile.launch(effect.suggestedName)
                ExportEffect.RequestAnkiDroidPermission -> requestAnkiPermission.launch(AnkiDroidApi.PERMISSION)
                is ExportEffect.Message -> chrome.snackbar.showSnackbar(effect.text)
            }
        }
    }

    val destination = when (ui.settings.anki.exportTarget) {
        ExportTarget.AUTO -> if (ui.ankiDroidInstalled) "AnkiDroid" else ".apkg file"
        ExportTarget.ANKIDROID -> if (ui.ankiDroidInstalled) "AnkiDroid" else ".apkg file (AnkiDroid not installed)"
        ExportTarget.APKG_FILE -> ".apkg file"
    }

    Scaffold(
        topBar = {
            StickyTopBar(
                title = "Export",
                actions = {
                    if (ui.notes.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        "Destination: $destination · deck “${ui.settings.anki.deckName}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::exportAll,
                        enabled = ui.notes.isNotEmpty() && !ui.exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (ui.exporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Export all to Anki (${ui.notes.size})")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (ui.notes.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Nothing queued yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap a word in the Reader or Grammar breakdown and choose “Add to Anki”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(ui.notes, key = { it.id }) { note ->
                    NoteRow(note = note, defaultsDeck = ui.settings.anki.deckName, onClick = { viewModel.edit(note) })
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }

    ui.editing?.let { note ->
        AnkiNoteSheet(
            draft = note,
            defaults = ui.settings.anki,
            confirmLabel = "Save",
            onDismiss = viewModel::dismissEdit,
            onConfirm = viewModel::saveEdit,
            onDelete = { viewModel.remove(note) },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear export heap?") },
            text = { Text("All ${ui.notes.size} queued cards will be removed.") },
            confirmButton = { TextButton(onClick = { viewModel.clearAll(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NoteRow(note: ExportNote, defaultsDeck: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(note.word, style = MaterialTheme.typography.titleMedium)
                if (note.reading.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(note.reading, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(note.meaning, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val extras = listOfNotNull(
                note.deckOverride?.takeIf { it != defaultsDeck }?.let { "deck: $it" },
                note.noteTypeOverride?.let { it.label.substringBefore(" (") },
                note.includeSentenceOverride?.let { if (it) "with sentence" else "no sentence" },
            )
            if (extras.isNotEmpty()) {
                Text(extras.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Text("Edit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
