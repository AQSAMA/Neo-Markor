package com.aqsama.neomarkor.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FolderMetadata
import com.aqsama.neomarkor.domain.model.FOLDER_PRESET_COLORS
import com.aqsama.neomarkor.presentation.viewmodel.FolderViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFoldersScreen(
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val foldersWithCounts by viewModel.foldersWithCounts.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createSubFolderParentId by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<FolderMetadata?>(null) }
    var showColorDialog by remember { mutableStateOf<FolderMetadata?>(null) }
    var showMoveDialog by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val noteCountMap = remember(foldersWithCounts) {
        foldersWithCounts.associate { (folder, count) -> folder.id to count }
    }

    val flatVisible = remember(uiState.folders, uiState.expandedIds) {
        viewModel.visibleFlatFolders(uiState.folders, uiState.expandedIds)
    }

    // Create sub-folder dialog trigger
    if (showCreateDialog || createSubFolderParentId != null) {
        CreateFolderDialog(
            initialParentId = createSubFolderParentId,
            onDismiss = {
                showCreateDialog = false
                createSubFolderParentId = null
            },
            onCreate = { name, color, parentId ->
                viewModel.createFolder(name, color, parentId)
                showCreateDialog = false
                createSubFolderParentId = null
            },
        )
    }

    showRenameDialog?.let { folder ->
        var newName by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename folder") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameFolder(folder.id, newName.trim())
                        showRenameDialog = null
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            },
        )
    }

    showColorDialog?.let { folder ->
        FolderColorPickerDialog(
            currentColor = folder.colorArgb,
            onDismiss = { showColorDialog = null },
            onColorSelected = { color ->
                viewModel.setFolderColor(folder.id, color)
                showColorDialog = null
            },
        )
    }

    if (showMoveDialog.isNotEmpty()) {
        MoveToFolderDialog(
            excludeIds = showMoveDialog,
            allFolders = uiState.folders,
            onDismiss = { showMoveDialog = emptySet() },
            onMove = { targetId ->
                showMoveDialog.forEach { id -> viewModel.moveFolder(id, targetId) }
                showMoveDialog = emptySet()
                viewModel.exitEditMode()
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete folders") },
            text = { Text("Delete ${uiState.selectedIds.size} folder(s)? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolders(uiState.selectedIds)
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditMode) viewModel.exitEditMode()
                        else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    if (uiState.isEditMode) {
                        Text("${uiState.selectedIds.size} selected")
                    } else {
                        Text("Manage folders")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        TextButton(onClick = {
                            if (uiState.selectedIds.size == uiState.folders.size) viewModel.clearSelection()
                            else viewModel.selectAll()
                        }) {
                            Icon(
                                if (uiState.selectedIds.size == uiState.folders.size)
                                    Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("All")
                        }
                    } else {
                        TextButton(onClick = { viewModel.enterEditMode() }) {
                            Text("Edit")
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = uiState.isEditMode) {
                EditModeActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onMove = { showMoveDialog = uiState.selectedIds },
                    onCreateSub = {
                        uiState.selectedIds.firstOrNull()?.let { id ->
                            createSubFolderParentId = id
                        }
                    },
                    onColor = {
                        uiState.selectedIds.firstOrNull()?.let { id ->
                            val folder = uiState.folders.find { it.id == id }
                            showColorDialog = folder
                        }
                    },
                    onRename = {
                        uiState.selectedIds.firstOrNull()?.let { id ->
                            val folder = uiState.folders.find { it.id == id }
                            showRenameDialog = folder
                        }
                    },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.folders.isEmpty()) {
                EmptyFoldersPlaceholder(
                    modifier = Modifier.align(Alignment.Center),
                    onCreateFolder = { showCreateDialog = true },
                )
            } else {
                ReorderableFolderList(
                    flatItems = flatVisible,
                    noteCountMap = noteCountMap,
                    isEditMode = uiState.isEditMode,
                    selectedIds = uiState.selectedIds,
                    expandedIds = uiState.expandedIds,
                    allFolders = uiState.folders,
                    onToggleExpanded = { viewModel.toggleExpanded(it) },
                    onToggleSelected = { viewModel.toggleSelected(it) },
                    onReorder = { orderedIds -> viewModel.reorderFolders(orderedIds) },
                    onCreateFolder = { showCreateDialog = true },
                )
            }
        }
    }
}

@Composable
private fun ReorderableFolderList(
    flatItems: List<Pair<FolderMetadata, Int>>,
    noteCountMap: Map<String, Int>,
    isEditMode: Boolean,
    selectedIds: Set<String>,
    expandedIds: Set<String>,
    allFolders: List<FolderMetadata>,
    onToggleExpanded: (String) -> Unit,
    onToggleSelected: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onCreateFolder: () -> Unit,
) {
    // Local mutable list for live drag reordering
    val mutableItems = remember(flatItems) { flatItems.toMutableStateList() }

    var draggingIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(mutableItems, key = { _, item -> item.first.id }) { index, (folder, depth) ->
            val hasChildren = allFolders.any { it.parentId == folder.id }
            val isExpanded = folder.id in expandedIds
            val isSelected = folder.id in selectedIds
            val isDragging = index == draggingIndex

            FolderManageItem(
                folder = folder,
                depth = depth,
                hasChildren = hasChildren,
                isExpanded = isExpanded,
                isEditMode = isEditMode,
                isSelected = isSelected,
                isDragging = isDragging,
                noteCount = noteCountMap[folder.id] ?: 0,
                onToggleExpanded = { onToggleExpanded(folder.id) },
                onToggleSelected = { onToggleSelected(folder.id) },
                onDragStart = { draggingIndex = index },
                onDragEnd = {
                    if (draggingIndex >= 0) {
                        onReorder(mutableItems.map { it.first.id })
                    }
                    draggingIndex = -1
                },
                onDrag = { delta ->
                    val targetIndex = (index + if (delta > 0) 1 else -1)
                        .coerceIn(0, mutableItems.lastIndex)
                    if (targetIndex != index) {
                        val item = mutableItems.removeAt(index)
                        mutableItems.add(targetIndex, item)
                        draggingIndex = targetIndex
                    }
                },
            )
        }

        item {
            // "Create folder" button at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateFolder() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Create folder",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FolderManageItem(
    folder: FolderMetadata,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isEditMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    noteCount: Int,
    onToggleExpanded: () -> Unit,
    onToggleSelected: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag_elevation")
    val folderColor = Color(folder.colorArgb)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .shadow(elevation, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (isEditMode) onToggleSelected() else onToggleExpanded()
            },
        color = if (isDragging)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: chevron (or placeholder)
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }

            Spacer(Modifier.width(4.dp))

            // Folder icon with color accent
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(10.dp))

            // Folder name
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Right side: note count + drag handle OR checkbox
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                )
            } else {
                if (noteCount > 0) {
                    Text(
                        text = noteCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { _, offset -> onDrag(offset.y) },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragEnd,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeActionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onCreateSub: () -> Unit,
    onColor: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val enabled = selectedCount > 0
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionBarItem(Icons.Default.DriveFileMove, "Move", enabled, onMove)
            ActionBarItem(Icons.Default.CreateNewFolder, "Create subfolder", selectedCount == 1, onCreateSub)
            ActionBarItem(Icons.Default.Palette, "Folder color", selectedCount == 1, onColor)
            ActionBarItem(Icons.Default.Edit, "Rename", selectedCount == 1, onRename)
            ActionBarItem(Icons.Default.Delete, "Delete", enabled, onDelete,
                tint = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ActionBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
) {
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun EmptyFoldersPlaceholder(
    modifier: Modifier = Modifier,
    onCreateFolder: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            "No folders yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreateFolder) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create folder")
        }
    }
}

@Composable
fun CreateFolderDialog(
    initialParentId: String? = null,
    onDismiss: () -> Unit,
    onCreate: (name: String, colorArgb: Int, parentId: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FOLDER_PRESET_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Color picker row
                Text("Color", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FOLDER_PRESET_COLORS.forEach { colorArgb ->
                        val color = Color(colorArgb)
                        val isSelected = colorArgb == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = colorArgb },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedColor, initialParentId)
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun FolderColorPickerDialog(
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder color") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FOLDER_PRESET_COLORS.forEach { colorArgb ->
                    val color = Color(colorArgb)
                    val isSelected = colorArgb == selected
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { selected = colorArgb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selected) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MoveToFolderDialog(
    excludeIds: Set<String>,
    allFolders: List<FolderMetadata>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column {
                TextButton(onClick = { onMove(null) }) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Root (no folder)")
                }
                allFolders.filter { it.id !in excludeIds }.forEach { folder ->
                    TextButton(onClick = { onMove(folder.id) }) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(folder.colorArgb),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
