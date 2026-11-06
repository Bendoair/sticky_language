package com.stickylanguage.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickylanguage.app.ui.components.StickyTopBar

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenExport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.onImagePicked(uri)
            onOpenReader()
        }
    }

    Scaffold(
        topBar = { StickyTopBar(title = "Sticky Language") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "What are you reading?",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "${ui.learned.englishName}  →  ${ui.explanation.englishName}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

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

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenExport),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (ui.exportCount == 0) "Export heap is empty" else "${ui.exportCount} cards ready to export",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (ui.ankiDroidInstalled) "AnkiDroid found on this phone" else "AnkiDroid not found · exports as .apkg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BigAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val container = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val content = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        tonalElevation = if (primary) 0.dp else 1.dp,
        shadowElevation = if (primary) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = if (primary) 22.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp).size(if (primary) 28.dp else 22.dp))
            }
            Column {
                Text(title, style = if (primary) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.7f))
            }
        }
    }
}
