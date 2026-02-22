package com.aqsama.neomarkor.ui.screen

import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.DragAndDropTarget
import androidx.compose.foundation.draganddrop.DragAndDropTransferData
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.mutableStateMapOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.presentation.viewmodel.FileBrowserViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    viewModel: FileBrowserViewModel = koinViewModel(),
) {
    val fileTree by viewModel.fileTree.collectAsState()
    val directoryUri by viewModel.directoryUri.collectAsState()
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    val descendantUriMap = remember(fileTree) { buildDescendantUriMap(fileTree) }

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
                            parentUri = directoryUri.orEmpty(),
                            depth = 0,
                            onOpenEditor = onOpenEditor,
                            isExpanded = { uri -> expandedMap[uri] == true },
                            onToggleExpanded = { uri -> expandedMap[uri] = !(expandedMap[uri] == true) },
                            onForceExpanded = { uri -> expandedMap[uri] = true },
                            onMoveNode = viewModel::moveNode,
                            isDescendantTarget = { sourceUri, targetUri ->
                                descendantUriMap[sourceUri]?.contains(targetUri) == true
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

@Composable
private fun FileTreeItem(
    file: FileNode,
    parentUri: String,
    depth: Int,
    onOpenEditor: (String) -> Unit,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
    onForceExpanded: (String) -> Unit,
    onMoveNode: (sourceUriString: String, sourceParentUriString: String, targetDirectoryUriString: String) -> Unit,
    isDescendantTarget: (sourceUriString: String, targetDirectoryUriString: String) -> Boolean,
) {
    val expanded = isExpanded(file.uriString)
    val rowColor = if (file.isDirectory) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.surface
    val itemModifier = Modifier
        .fillMaxWidth()
        .dragAndDropSource {
            detectTapGestures(onLongPress = {
                startTransfer(
                    DragAndDropTransferData(
                        clipData = ClipData.newPlainText(
                            "neo_markor_node",
                            encodeDragPayload(file.uriString, parentUri, file.isDirectory)
                        )
                    )
                )
            })
        }
        .then(
            if (file.isDirectory) {
                Modifier.dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        decodeDragPayload(event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()) != null
                    },
                    target = remember(file.uriString) {
                        object : DragAndDropTarget {
                            override fun onDrop(event: androidx.compose.ui.draganddrop.DragAndDropEvent): Boolean {
                                val payload = decodeDragPayload(
                                    event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ) ?: return false
                                if (!canDropOnTarget(payload, file.uriString, isDescendantTarget)) return false
                                onForceExpanded(file.uriString)
                                onMoveNode(payload.sourceUriString, payload.sourceParentUriString, file.uriString)
                                return true
                            }
                        }
                    }
                )
            } else {
                Modifier
            }
        )

    Column {
        Surface(
            modifier = itemModifier
                .clickable {
                    if (file.isDirectory) onToggleExpanded(file.uriString)
                    else onOpenEditor(file.uriString)
                },
            shape = RoundedCornerShape(8.dp),
            color = rowColor,
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
                    parentUri = file.uriString,
                    depth = depth + 1,
                    onOpenEditor = onOpenEditor,
                    isExpanded = isExpanded,
                    onToggleExpanded = onToggleExpanded,
                    onForceExpanded = onForceExpanded,
                    onMoveNode = onMoveNode,
                    isDescendantTarget = isDescendantTarget,
                )
            }
        }
    }
}

@Serializable
internal data class DragNodePayload(
    val sourceUriString: String,
    val sourceParentUriString: String,
    val sourceIsDirectory: Boolean,
)

internal fun encodeDragPayload(
    sourceUriString: String,
    sourceParentUriString: String,
    sourceIsDirectory: Boolean
): String = Json.encodeToString(
    DragNodePayload(
        sourceUriString = sourceUriString,
        sourceParentUriString = sourceParentUriString,
        sourceIsDirectory = sourceIsDirectory
    )
)

internal fun decodeDragPayload(raw: String?): DragNodePayload? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Json.decodeFromString<DragNodePayload>(raw) }.getOrNull()
}

internal fun canDropOnTarget(payload: DragNodePayload, targetDirectoryUriString: String): Boolean {
    return canDropOnTarget(payload, targetDirectoryUriString) { _, _ -> false }
}

internal fun canDropOnTarget(
    payload: DragNodePayload,
    targetDirectoryUriString: String,
    isDescendantTarget: (sourceUriString: String, targetDirectoryUriString: String) -> Boolean
): Boolean {
    if (payload.sourceUriString == targetDirectoryUriString) return false
    if (payload.sourceParentUriString == targetDirectoryUriString) return false
    if (payload.sourceIsDirectory && isDescendantTarget(payload.sourceUriString, targetDirectoryUriString)) return false
    return true
}

internal fun buildDescendantUriMap(nodes: List<FileNode>): Map<String, Set<String>> {
    val map = mutableMapOf<String, Set<String>>()
    nodes.forEach { node -> collectDescendantUris(node, map) }
    return map
}

private fun collectDescendantUris(node: FileNode, map: MutableMap<String, Set<String>>): Set<String> {
    val descendants = mutableSetOf<String>()
    node.children.forEach { child ->
        descendants += child.uriString
        descendants += collectDescendantUris(child, map)
    }
    map[node.uriString] = descendants
    return descendants
}
