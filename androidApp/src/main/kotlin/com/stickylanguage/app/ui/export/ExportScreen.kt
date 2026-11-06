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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.stickylanguage.app.ui.components.DashedRule
import com.stickylanguage.app.ui.components.StickyBottomBar
import com.stickylanguage.app.ui.components.StickyButton
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickyIconButton
import com.stickylanguage.app.ui.components.StickyTextButton
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.app.ui.components.stickyContentInsets
import com.stickylanguage.app.ui.theme.sticky
import com.stickylanguage.shared.anki.ExportNote
import com.stickylanguage.shared.anki.ExportTarget

@Composable
fun ExportScreen(viewModel: ExportViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chrome = LocalAppChrome.current
    val c = MaterialTheme.sticky
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
                        StickyIconButton(Icons.Outlined.DeleteSweep, "Clear all", onClick = { confirmClear = true })
                    }
                },
            )
        },
        bottomBar = {
            StickyBottomBar {
                Column {
                    Text(
                        "Destination: $destination · deck “${ui.settings.anki.deckName}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.mid,
                    )
                    Spacer(Modifier.height(8.dp))
                    StickyButton(
                        primary = true,
                        onClick = viewModel::exportAll,
                        enabled = ui.notes.isNotEmpty() && !ui.exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (ui.exporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.paper)
                        } else {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Export all to Anki (${ui.notes.size})")
                    }
                }
            }
        },
        contentWindowInsets = stickyContentInsets(),
        containerColor = c.paper,
    ) { padding ->
        if (ui.notes.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                StickyCard(dashed = true, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Nothing queued yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap a word in the Reader or Grammar breakdown and choose “Add to Anki”.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mid,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(ui.notes, key = { it.id }) { note ->
                    NoteRow(note = note, defaultsDeck = ui.settings.anki.deckName, onClick = { viewModel.edit(note) })
                    DashedRule()
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
            confirmButton = { StickyTextButton("Clear", onClick = { viewModel.clearAll(); confirmClear = false }) },
            dismissButton = { StickyTextButton("Cancel", onClick = { confirmClear = false }) },
            containerColor = c.paper,
            titleContentColor = c.ink,
            textContentColor = c.ink,
            shape = MaterialTheme.shapes.small,
        )
    }
}

/** Prototype `.list li`: word / meaning on the left, "Edit" link on the right. */
@Composable
private fun NoteRow(note: ExportNote, defaultsDeck: String, onClick: () -> Unit) {
    val c = MaterialTheme.sticky
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(note.word, style = MaterialTheme.typography.titleMedium)
                if (note.reading.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(note.reading, style = MaterialTheme.typography.bodySmall, color = c.mid)
                }
            }
            Text(note.meaning, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val extras = listOfNotNull(
                note.deckOverride?.takeIf { it != defaultsDeck }?.let { "deck: $it" },
                note.noteTypeOverride?.let { it.label.substringBefore(" (") },
                note.includeSentenceOverride?.let { if (it) "with sentence" else "no sentence" },
            )
            if (extras.isNotEmpty()) {
                Text(extras.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = c.mid)
            }
        }
        StickyTextButton("Edit", onClick = onClick)
    }
}
