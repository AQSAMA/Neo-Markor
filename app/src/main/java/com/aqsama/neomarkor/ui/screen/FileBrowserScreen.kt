package com.aqsama.neomarkor.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.presentation.viewmodel.FileBrowserViewModel
import org.koin.androidx.compose.koinViewModel

/** State representing a file currently being dragged. */
private data class FileDragState(
    val fileUri: String,
    val fileName: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    viewModel: FileBrowserViewModel = koinViewModel(),
) {
    val fileTree by viewModel.fileTree.collectAsState()
    val directoryUri by viewModel.directoryUri.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }

    // Drag-and-drop state
    var dragState by remember { mutableStateOf<FileDragState?>(null) }
    var hoverDirUri by remember { mutableStateOf<String?>(null) }
    // Non-state map to avoid unnecessary recompositions on position updates
    val dirPositions = remember { mutableMapOf<String, Rect>() }

    val allDirectories = remember(fileTree) { collectAllDirectories(fileTree) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.setDirectory(uri.toString())
    }

    // New Folder dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName.trim())
                            showNewFolderDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            },
        )
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
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                    }
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
                            text = "No supported files found in selected directory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(fileTree, key = { it.uriString }) { file ->
                        FileTreeItem(
                            file = file,
                            depth = 0,
                            onOpenEditor = onOpenEditor,
                            onDelete = { viewModel.deleteFile(it) },
                            onRename = { uri, name -> viewModel.renameFile(uri, name) },
                            onMove = { src, dst -> viewModel.moveFile(src, dst) },
                            allDirectories = allDirectories,
                            dragState = dragState,
                            hoverDirUri = hoverDirUri,
                            onFileDragStart = { fileNode ->
                                if (!fileNode.isDirectory) {
                                    dragState = FileDragState(fileNode.uriString, fileNode.name)
                                    hoverDirUri = null
                                }
                            },
                            onDragUpdate = { globalOffset ->
                                hoverDirUri = dirPositions.entries
                                    .firstOrNull { (_, rect) -> rect.contains(globalOffset) }
                                    ?.key
                            },
                            onFileDragEnd = {
                                val srcUri = dragState?.fileUri
                                val dstUri = hoverDirUri
                                if (srcUri != null && dstUri != null) {
                                    viewModel.moveFile(srcUri, dstUri)
                                }
                                dragState = null
                                hoverDirUri = null
                            },
                            onRegisterDir = { uri, rect -> dirPositions[uri] = rect },
                        )
                    }
                }
            }
        }
    }
}

/** Recursively collects all directory nodes from the file tree. */
private fun collectAllDirectories(nodes: List<FileNode>): List<FileNode> =
    nodes.flatMap { node ->
        if (node.isDirectory) listOf(node) + collectAllDirectories(node.children)
        else emptyList()
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
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onMove: (String, String) -> Unit,
    allDirectories: List<FileNode>,
    dragState: FileDragState?,
    hoverDirUri: String?,
    onFileDragStart: (FileNode) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onFileDragEnd: () -> Unit,
    onRegisterDir: (String, Rect) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isDraggingThis = dragState?.fileUri == file.uriString
    val isDropTarget = file.isDirectory && hoverDirUri == file.uriString

    // Rename dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onRename(file.uriString, newName.trim())
                        showRenameDialog = false
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Move dialog — shows all directories in the tree
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to folder") },
            text = {
                Column {
                    val movableDirectories = allDirectories.filter { it.uriString != file.uriString }
                    if (movableDirectories.isEmpty()) {
                        Text("No folders available. Create a folder first.")
                    } else {
                        movableDirectories.forEach { dir ->
                            TextButton(onClick = {
                                onMove(file.uriString, dir.uriString)
                                showMoveDialog = false
                            }) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dir.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete") },
            text = { Text("Delete \"${file.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(file.uriString)
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (file.isDirectory) {
                        // Register bounds for drop-target detection and handle tap to expand
                        Modifier
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                val sz = coords.size
                                onRegisterDir(
                                    file.uriString,
                                    Rect(pos, Size(sz.width.toFloat(), sz.height.toFloat())),
                                )
                            }
                            .combinedClickable(
                                onClick = { expanded = !expanded },
                                onLongClick = { showContextMenu = true },
                            )
                    } else {
                        // Files open on tap
                        Modifier.clickable { onOpenEditor(file.uriString) }
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            color = when {
                isDropTarget -> MaterialTheme.colorScheme.primaryContainer
                file.isDirectory -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            },
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = (16 + depth * 16).dp,
                        end = 8.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    )
                    .alpha(if (isDraggingThis) 0.35f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Dedicated expand/collapse button for directories
                if (file.isDirectory) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = when {
                        file.isDirectory && expanded -> Icons.Default.FolderOpen
                        file.isDirectory -> Icons.Default.Folder
                        file.name.endsWith(".md") -> Icons.Default.Description
                        file.name.endsWith(".json") -> Icons.Default.DataObject
                        file.name.endsWith(".yaml") || file.name.endsWith(".yml") -> Icons.Default.Settings
                        else -> Icons.Default.Article
                    },
                    contentDescription = null,
                    tint = when {
                        isDropTarget -> MaterialTheme.colorScheme.onPrimaryContainer
                        file.isDirectory -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Drag handle — long-press to drag a file into a folder
                if (!file.isDirectory) {
                    var handleGlobalPos by remember { mutableStateOf(Offset.Zero) }
                    var dragStartLocal by remember { mutableStateOf(Offset.Zero) }
                    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords ->
                                handleGlobalPos = coords.positionInWindow()
                            }
                            .pointerInput(file.uriString) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        dragStartLocal = localOffset
                                        dragAccumulated = Offset.Zero
                                        onFileDragStart(file)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragAccumulated += amount
                                        onDragUpdate(handleGlobalPos + dragStartLocal + dragAccumulated)
                                    },
                                    onDragEnd = onFileDragEnd,
                                    onDragCancel = onFileDragEnd,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Long-press to drag file to a folder",
                            tint = if (isDraggingThis)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Context menu button
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                showContextMenu = false
                                showRenameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        )
                        if (!file.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("Move to…") },
                                onClick = {
                                    showContextMenu = false
                                    showMoveDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showContextMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        }

        // Expanded children
        if (file.isDirectory && expanded) {
            file.children.forEach { child ->
                FileTreeItem(
                    file = child,
                    depth = depth + 1,
                    onOpenEditor = onOpenEditor,
                    onDelete = onDelete,
                    onRename = onRename,
                    onMove = onMove,
                    allDirectories = allDirectories,
                    dragState = dragState,
                    hoverDirUri = hoverDirUri,
                    onFileDragStart = onFileDragStart,
                    onDragUpdate = onDragUpdate,
                    onFileDragEnd = onFileDragEnd,
                    onRegisterDir = onRegisterDir,
                )
            }
        }
    }
}
