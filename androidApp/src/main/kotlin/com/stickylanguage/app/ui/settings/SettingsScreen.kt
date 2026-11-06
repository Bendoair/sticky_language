package com.stickylanguage.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.BuildConfig
import com.stickylanguage.app.data.settings.ThemeMode
import com.stickylanguage.app.llm.local.ModelDownloadState
import com.stickylanguage.app.ui.components.DashedRule
import com.stickylanguage.app.ui.components.SectionLabel
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickySegmented
import com.stickylanguage.app.ui.components.StickyTextButton
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.app.ui.components.stickyContentInsets
import com.stickylanguage.app.ui.components.stickyRadioColors
import com.stickylanguage.app.ui.components.stickySwitchColors
import com.stickylanguage.app.ui.components.stickyTextFieldColors
import com.stickylanguage.app.ui.theme.sticky
import com.stickylanguage.shared.anki.AnkiNoteType
import com.stickylanguage.shared.anki.ExportTarget
import com.stickylanguage.shared.llm.LlmProvider
import com.stickylanguage.shared.model.Language

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val s = ui.settings
    val c = MaterialTheme.sticky
    var pickLearned by remember { mutableStateOf(false) }
    var pickExplanation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { StickyTopBar(title = "Settings") },
        contentWindowInsets = stickyContentInsets(),
        containerColor = c.paper,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // ---- Languages ----
            SectionLabel("Languages")
            Spacer(Modifier.height(6.dp))
            StickyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                Column {
                    SettingRow(title = "I'm learning", value = s.learned.displayName) { pickLearned = true }
                    DashedRule()
                    SettingRow(title = "Explain in", value = s.explanation.displayName) { pickExplanation = true }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- LLM provider ----
            SectionLabel("Translation model (LLM)")
            Spacer(Modifier.height(4.dp))
            Text(
                "Cloud uses an OpenAI-compatible API. On-device downloads a small model and runs entirely offline.",
                style = MaterialTheme.typography.bodySmall,
                color = c.mid,
            )
            Spacer(Modifier.height(10.dp))
            StickySegmented(
                options = LlmProvider.entries.map { it.label },
                selectedIndex = LlmProvider.entries.indexOf(s.llmProvider),
                onSelect = { viewModel.setProvider(LlmProvider.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )

            when (s.llmProvider) {
                LlmProvider.CLOUD -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (s.hasBuildTimeKey) "A default API key is built into this app. Enter your own to override it."
                        else "No default key is built in. Enter an API key for an OpenAI-compatible endpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.mid,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecretField(
                        label = "API key",
                        value = s.apiKeyOverride,
                        placeholder = if (s.hasBuildTimeKey) "Using built-in key" else "sk-…",
                        onCommit = viewModel::setApiKey,
                    )
                    Spacer(Modifier.height(10.dp))
                    CommitTextField(
                        label = "Base URL",
                        value = s.baseUrlOverride,
                        placeholder = BuildConfig.LLM_BASE_URL,
                        onCommit = viewModel::setBaseUrl,
                    )
                    Spacer(Modifier.height(10.dp))
                    CommitTextField(
                        label = "Model",
                        value = s.modelOverride,
                        placeholder = BuildConfig.LLM_MODEL,
                        onCommit = viewModel::setModel,
                    )
                }

                LlmProvider.LOCAL -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Device RAM ~${ui.deviceRamMb} MB · ${ui.freeStorageLabel}. Models stay in app storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.mid,
                    )
                    Spacer(Modifier.height(10.dp))
                    ui.localModels.forEach { row ->
                        LocalModelCard(
                            row = row,
                            download = ui.download,
                            onSelect = { viewModel.selectLocalModel(row.spec.id) },
                            onDownload = { viewModel.downloadModel(row.spec) },
                            onCancel = viewModel::cancelDownload,
                            onDelete = { viewModel.deleteModel(row.spec) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Anki ----
            SectionLabel("Anki")
            Spacer(Modifier.height(10.dp))
            CommitTextField(
                label = "Deck name",
                value = s.anki.deckName,
                placeholder = "Sticky Language",
                onCommit = { viewModel.setDeck(it.ifBlank { "Sticky Language" }) },
            )
            Spacer(Modifier.height(14.dp))
            Text("Note type", style = MaterialTheme.typography.titleSmall)
            AnkiNoteType.entries.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setNoteType(t) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.anki.noteType == t, onClick = { viewModel.setNoteType(t) }, colors = stickyRadioColors())
                    Column {
                        Text(t.label.substringBefore(" ("), style = MaterialTheme.typography.bodyMedium)
                        Text(t.fields.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = c.mid)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            ToggleRow(title = "Include source sentence", checked = s.anki.includeSentence, onChange = viewModel::setIncludeSentence)
            Spacer(Modifier.height(10.dp))
            Text("Export place", style = MaterialTheme.typography.titleSmall)
            Text(
                if (viewModel.ankiDroidInstalled) "AnkiDroid is installed on this phone." else "AnkiDroid is not installed; files are saved as .apkg.",
                style = MaterialTheme.typography.bodySmall,
                color = c.mid,
            )
            ExportTarget.entries.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setExportTarget(t) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.anki.exportTarget == t, onClick = { viewModel.setExportTarget(t) }, colors = stickyRadioColors())
                    Text(t.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Appearance ----
            SectionLabel("Appearance")
            Spacer(Modifier.height(10.dp))
            StickySegmented(
                options = ThemeMode.entries.map { it.label },
                selectedIndex = ThemeMode.entries.indexOf(s.themeMode),
                onSelect = { viewModel.setTheme(ThemeMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (pickLearned) {
        LanguagePicker(title = "I'm learning", selected = s.learned, onDismiss = { pickLearned = false }) {
            viewModel.setLearned(it); pickLearned = false
        }
    }
    if (pickExplanation) {
        LanguagePicker(title = "Explain in", selected = s.explanation, onDismiss = { pickExplanation = false }) {
            viewModel.setExplanation(it); pickExplanation = false
        }
    }
}

@Composable
private fun LocalModelCard(
    row: LocalModelRow,
    download: ModelDownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.sticky
    val spec = row.spec
    val downloadingThis = when (download) {
        is ModelDownloadState.Downloading -> download.modelId == spec.id
        is ModelDownloadState.Verifying -> download.modelId == spec.id
        else -> false
    }
    val failedThis = (download as? ModelDownloadState.Failed)?.takeIf { it.modelId == spec.id }

    StickyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = row.downloaded, onClick = onSelect),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = row.selected && row.downloaded,
                    onClick = if (row.downloaded) onSelect else null,
                    enabled = row.downloaded,
                    colors = stickyRadioColors(),
                )
                Column(Modifier.weight(1f)) {
                    Text(spec.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${spec.sizeLabel} · ${spec.quant}" + if (!row.ramOk) " · needs ~${spec.minRamMb / 1024} GB RAM" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.ramOk) c.mid else c.danger,
                    )
                    Text(spec.description, style = MaterialTheme.typography.bodySmall, color = c.mid)
                }
            }

            when {
                downloadingThis && download is ModelDownloadState.Downloading -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = c.ink,
                        trackColor = c.fill,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${(download.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        StickyTextButton("Cancel", onClick = onCancel)
                    }
                }

                downloadingThis && download is ModelDownloadState.Verifying -> {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = c.ink)
                        Spacer(Modifier.width(8.dp))
                        Text("Verifying checksum…", style = MaterialTheme.typography.labelMedium)
                    }
                }

                failedThis != null -> {
                    Spacer(Modifier.height(6.dp))
                    Text(failedThis.message, style = MaterialTheme.typography.labelMedium, color = c.danger)
                    Spacer(Modifier.height(4.dp))
                    StickyTextButton("Retry download", onClick = onDownload)
                }

                row.downloaded -> {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        if (row.selected) {
                            Text("Active", style = MaterialTheme.typography.labelMedium, color = c.ink, modifier = Modifier.weight(1f))
                        } else {
                            StickyTextButton("Use", onClick = onSelect)
                            Spacer(Modifier.weight(1f))
                        }
                        StickyTextButton("Delete", onClick = onDelete)
                    }
                }

                else -> {
                    Spacer(Modifier.height(4.dp))
                    StickyTextButton("Download ${spec.sizeLabel}", onClick = onDownload)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.sticky.ink)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = stickySwitchColors())
    }
}

/** Text field that writes to settings when focus leaves (avoids a DataStore write per keystroke). */
@Composable
private fun CommitTextField(
    label: String,
    value: String,
    placeholder: String,
    onCommit: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = stickyTextFieldColors(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused && text != value) onCommit(text) },
    )
}

@Composable
private fun SecretField(label: String, value: String, placeholder: String, onCommit: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    CommitTextField(
        label = label,
        value = value,
        placeholder = placeholder,
        onCommit = onCommit,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle visibility")
            }
        },
    )
}

@Composable
private fun LanguagePicker(title: String, selected: Language, onDismiss: () -> Unit, onPick: (Language) -> Unit) {
    val c = MaterialTheme.sticky
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.height(380.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(Language.all, key = { it.code }) { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(lang) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = lang == selected, onClick = { onPick(lang) }, colors = stickyRadioColors())
                        Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { StickyTextButton("Close", onClick = onDismiss) },
        containerColor = c.paper,
        titleContentColor = c.ink,
        textContentColor = c.ink,
        shape = MaterialTheme.shapes.small,
    )
}
