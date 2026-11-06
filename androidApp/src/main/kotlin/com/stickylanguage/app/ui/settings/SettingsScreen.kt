package com.stickylanguage.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.stickylanguage.shared.model.Language

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
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
            StickyCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                Column {
                    SettingRow(title = "I'm learning", value = s.learned.displayName) { pickLearned = true }
                    DashedRule()
                    SettingRow(title = "Explain in", value = s.explanation.displayName) { pickExplanation = true }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- LLM ----
            SectionLabel("Translation model (LLM)")
            Spacer(Modifier.height(4.dp))
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
