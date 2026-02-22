package com.aqsama.neomarkor.ui.screen

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.presentation.viewmodel.FileBrowserViewModel
import org.koin.androidx.compose.koinViewModel

/** Separator used to pack source URI + parent URI into a single ClipData text item. */
private const val DND_SEPARATOR = "\u001F"

/**
 * Extracts (sourceUri, sourceParentUri) from a drag-and-drop [event].
 * Returns null if the ClipData is missing or malformed.
 */
private fun parseDragEvent(event: DragAndDropEvent): Pair<String, String>? {
    val text = event.toAndroidDragEvent()
        .clipData?.getItemAt(0)?.text?.toString() ?: return null
    val parts = text.split(DND_SEPARATOR)
    val sourceUri = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
    val sourceParentUri = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return null
    return sourceUri to sourceParentUri
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    viewModel: FileBrowserViewModel = koinViewModel(),
) {
    val fileTree by viewModel.fileTree.collectAsState()
    val directoryUri by viewModel.directoryUri.collectAsState()

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.setDirectory(uri.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { dirPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Choose directory")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (directoryUri == null) {
            NoDirPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onPickDirectory = { dirPickerLauncher.launch(null) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (fileTree.isEmpty()) {
                    item {
                        Text(
                            text = "No .md or .txt files found in selected directory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(fileTree) { file ->
                        FileTreeItem(
                            file = file,
                            depth = 0,
                            onOpenEditor = onOpenEditor,
                            onMoveNode = { sourceUri, sourceParentUri, targetParentUri ->
                                viewModel.moveNode(sourceUri, sourceParentUri, targetParentUri)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoDirPlaceholder(modifier: Modifier = Modifier, onPickDirectory: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No workspace selected",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose a folder to browse your Markdown and text files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPickDirectory) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Directory")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeItem(
    file: FileNode,
    depth: Int,
    onOpenEditor: (String) -> Unit,
    onMoveNode: (sourceUri: String, sourceParentUri: String, targetParentUri: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // True while a drag is hovering over this folder.
    val isDropTarget = remember { mutableStateOf(false) }

    // Stable drop-target object; captures isDropTarget by reference so callbacks update UI.
    val dropTarget = remember(file.uriString) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDropTarget.value = false
                val (sourceUri, sourceParentUri) = parseDragEvent(event) ?: return false
                // Prevent dropping a node onto itself or into its current parent.
                // Note: moving a folder into one of its own descendants is rejected by the
                // SAF provider (DocumentsContract.moveDocument returns null), so we rely on
                // that graceful failure rather than walking the full subtree here.
                if (sourceUri == file.uriString || sourceParentUri == file.uriString) return false
                onMoveNode(sourceUri, sourceParentUri, file.uriString)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) { isDropTarget.value = true }
            override fun onExited(event: DragAndDropEvent) { isDropTarget.value = false }
            override fun onEnded(event: DragAndDropEvent) { isDropTarget.value = false }
        }
    }

    // Drag-source modifier: long-press starts a drag carrying the node's URI and parent URI.
    // Keyed on the URIs so it is only recreated when the node identity changes.
    val dragSourceModifier = remember(file.uriString, file.parentUriString) {
        Modifier.dragAndDropSource {
            detectTapGestures(
                onLongPress = {
                    startTransfer(
                        DragAndDropTransferData(
                            clipData = ClipData.newPlainText(
                                "neomarkor_node",
                                "${file.uriString}$DND_SEPARATOR${file.parentUriString ?: ""}",
                            ),
                        ),
                    )
                },
            )
        }
    }

    // Drop-target modifier applied only to directory nodes.
    val dropTargetModifier = if (file.isDirectory) {
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
            },
            target = dropTarget,
        )
    } else {
        Modifier
    }

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragSourceModifier)
                .then(dropTargetModifier)
                .clickable {
                    if (file.isDirectory) expanded = !expanded
                    else onOpenEditor(file.uriString)
                },
            shape = RoundedCornerShape(8.dp),
            color = when {
                isDropTarget.value -> MaterialTheme.colorScheme.primaryContainer
                file.isDirectory -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            },
            border = if (isDropTarget.value) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = (16 + depth * 16).dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        file.isDirectory && expanded -> Icons.Default.FolderOpen
                        file.isDirectory -> Icons.Default.Folder
                        file.name.endsWith(".md") -> Icons.Default.Description
                        else -> Icons.Default.Article
                    },
                    contentDescription = null,
                    tint = if (file.isDirectory)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (file.isDirectory) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (file.isDirectory && expanded) {
            file.children.forEach { child ->
                FileTreeItem(
                    file = child,
                    depth = depth + 1,
                    onOpenEditor = onOpenEditor,
                    onMoveNode = onMoveNode,
                )
            }
        }
    }
}
