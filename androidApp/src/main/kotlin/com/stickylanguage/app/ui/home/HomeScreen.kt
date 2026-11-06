package com.stickylanguage.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.ui.components.StickyCard
import com.stickylanguage.app.ui.components.StickyTopBar
import com.stickylanguage.app.ui.components.stickyContentInsets
import com.stickylanguage.app.ui.theme.sticky

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenExport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val c = MaterialTheme.sticky

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.onImagePicked(uri)
            onOpenReader()
        }
    }

    Scaffold(
        topBar = { StickyTopBar(title = "Sticky Language") },
        contentWindowInsets = stickyContentInsets(),
        containerColor = c.paper,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("What are you reading?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            // Prototype `.lang-pill`: bordered pill with the learned → explanation pair.
            Text(
                "${ui.learned.englishName}  →  ${ui.explanation.englishName}",
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
                modifier = Modifier
                    .border(2.dp, c.ink, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(28.dp))

            BigAction(
                icon = Icons.Outlined.PhotoCamera,
                title = "Scan",
                subtitle = "Point the camera at a page, sign or menu",
                primary = true,
                onClick = onScan,
            )
            Spacer(Modifier.height(12.dp))
            BigAction(
                icon = Icons.Outlined.Image,
                title = "Upload a picture",
                subtitle = "Photo or screenshot from your gallery",
                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
            Spacer(Modifier.height(12.dp))
            BigAction(
                icon = Icons.Outlined.Keyboard,
                title = "Type",
                subtitle = "Paste or type text to translate",
                onClick = {
                    viewModel.startTyping()
                    onOpenReader()
                },
            )

            Spacer(Modifier.height(32.dp))

            StickyCard(dashed = true, onClick = onOpenExport, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (ui.exportCount == 0) "Export heap is empty" else "${ui.exportCount} cards ready to export",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (ui.ankiDroidInstalled) "AnkiDroid found on this phone" else "AnkiDroid not found · exports as .apkg",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.mid,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.ink)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Prototype `.big-action`: 3dp ink border, 12dp radius; the primary one is sticky yellow. */
@Composable
private fun BigAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val c = MaterialTheme.sticky
    StickyCard(
        highlighted = primary,
        borderWidth = 3.dp,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (primary) 20.dp else 14.dp),
    ) {
        // StickyCard provides the right foreground for yellow vs paper (matters in dark mode).
        val fg = LocalContentColor.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(if (primary) 52.dp else 44.dp)
                    .border(2.dp, fg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (primary) 26.dp else 22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = if (primary) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, color = fg)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (primary) fg.copy(alpha = 0.75f) else c.mid)
            }
            Spacer(Modifier.width(2.dp))
        }
    }
}
