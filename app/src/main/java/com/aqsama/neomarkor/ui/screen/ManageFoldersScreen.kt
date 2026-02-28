package com.aqsama.neomarkor.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.Folder
import com.aqsama.neomarkor.domain.model.FolderColors
import com.aqsama.neomarkor.presentation.viewmodel.FolderViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFoldersScreen(
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = koinViewModel(),
) {
    val folders by viewModel.folders.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedIds by viewModel.selectedFolderIds.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createSubParentId by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetId by remember { mutableStateOf("") }
    var renameTargetName by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Create folder dialog
    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = {
                showCreateDialog = false
                createSubParentId = null
            },
            onConfirm = { name, color ->
                viewModel.createFolder(name, color, createSubParentId)
                showCreateDialog = false
                createSubParentId = null
            },
        )
    }

    // Rename dialog
    if (showRenameDialog) {
        RenameFolderDialog(
            currentName = renameTargetName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameFolder(renameTargetId, newName)
                showRenameDialog = false
            },
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        FolderColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                viewModel.setFoldersColor(selectedIds, color)
                showColorPicker = false
            },
        )
    }

    // Move dialog
    if (showMoveDialog) {
        MoveFolderDialog(
            folders = folders,
            excludeIds = selectedIds,
            onDismiss = { showMoveDialog = false },
            onMoveToRoot = {
                viewModel.moveFolders(selectedIds, null)
                showMoveDialog = false
            },
            onMoveToFolder = { targetId ->
                viewModel.moveFolders(selectedIds, targetId)
                showMoveDialog = false
            },
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Folders") },
            text = { Text("Delete ${selectedIds.size} selected folder(s)? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolders(selectedIds)
                    showDeleteConfirm = false
                    viewModel.exitEditMode()
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
                title = {
                    if (isEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    } else {
                        Text(
                            "Manage folders",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                navigationIcon = {
                    if (isEditMode) {
                        IconButton(onClick = {
                            if (selectedIds.size == folders.size) {
                                viewModel.deselectAll()
                            } else {
                                viewModel.selectAll()
                            }
                        }) {
                            Icon(
                                if (selectedIds.size == folders.size) Icons.Default.CheckCircle
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Select all",
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (isEditMode) viewModel.exitEditMode() else viewModel.toggleEditMode()
                    }) {
                        Text(
                            if (isEditMode) "Done" else "Edit",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (isEditMode) {
                EditModeBottomBar(
                    hasSelection = selectedIds.isNotEmpty(),
                    onMove = { showMoveDialog = true },
                    onCreateSub = {
                        if (selectedIds.size == 1) {
                            createSubParentId = selectedIds.first()
                            showCreateDialog = true
                        }
                    },
                    onFolderColor = { showColorPicker = true },
                    onRename = {
                        if (selectedIds.size == 1) {
                            val folder = folders.find { it.id == selectedIds.first() }
                            if (folder != null) {
                                renameTargetId = folder.id
                                renameTargetName = folder.name
                                showRenameDialog = true
                            }
                        }
                    },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        val rootFolders = viewModel.getRootFolders(folders)
                        if (rootFolders.isEmpty() && !isEditMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No folders yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            rootFolders.forEach { folder ->
                                FolderTreeItem(
                                    folder = folder,
                                    allFolders = folders,
                                    depth = 0,
                                    isEditMode = isEditMode,
                                    selectedIds = selectedIds,
                                    onToggleSelection = { viewModel.toggleSelection(it) },
                                    viewModel = viewModel,
                                )
                            }
                        }

                        // Create folder button
                        if (!isEditMode) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Create folder",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTreeItem(
    folder: Folder,
    allFolders: List<Folder>,
    depth: Int,
    isEditMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    viewModel: FolderViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val children = viewModel.getChildFolders(folder.id, allFolders)
    val hasChildren = children.isNotEmpty()
    val noteCount = viewModel.countNotesInSubtree(folder.id, allFolders)
    val isSelected = folder.id in selectedIds

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isEditMode) onToggleSelection(folder.id)
                    else if (hasChildren) expanded = !expanded
                }
                .padding(
                    start = (16 + depth * 24).dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditMode) {
                // Checkbox for selection
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleSelection(folder.id) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (hasChildren) {
                // Expand/collapse chevron
                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { expanded = !expanded },
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Folder icon with color
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color(folder.color),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Folder name
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Note count badge
            Text(
                text = "$noteCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!isEditMode) {
                Spacer(modifier = Modifier.width(8.dp))
                // Reorder handle
                Icon(
                    Icons.Default.UnfoldMore,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Children (if expanded)
        AnimatedVisibility(visible = expanded && !isEditMode || isEditMode) {
            Column {
                children.forEach { child ->
                    FolderTreeItem(
                        folder = child,
                        allFolders = allFolders,
                        depth = depth + 1,
                        isEditMode = isEditMode,
                        selectedIds = selectedIds,
                        onToggleSelection = onToggleSelection,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeBottomBar(
    hasSelection: Boolean,
    onMove: () -> Unit,
    onCreateSub: () -> Unit,
    onFolderColor: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomBarAction(
                icon = Icons.Default.DriveFileMove,
                label = "Move",
                enabled = hasSelection,
                onClick = onMove,
            )
            BottomBarAction(
                icon = Icons.Default.CreateNewFolder,
                label = "Create sub…",
                enabled = hasSelection,
                onClick = onCreateSub,
            )
            BottomBarAction(
                icon = Icons.Default.Palette,
                label = "Folder color",
                enabled = hasSelection,
                onClick = onFolderColor,
            )
            BottomBarAction(
                icon = Icons.Default.Edit,
                label = "Rename",
                enabled = hasSelection,
                onClick = onRename,
            )
            BottomBarAction(
                icon = Icons.Default.Delete,
                label = "Delete",
                enabled = hasSelection,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FolderColors.colors.first().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(FolderColors.colors) { (_, color) ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape,
                                    )
                                    else Modifier
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
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
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim(), selectedColor)
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) onConfirm(newName.trim())
                },
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun FolderColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder color") },
        text = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FolderColors.colors) { (name, color) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center,
                    ) {}
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MoveFolderDialog(
    folders: List<Folder>,
    excludeIds: Set<String>,
    onDismiss: () -> Unit,
    onMoveToRoot: () -> Unit,
    onMoveToFolder: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column {
                TextButton(onClick = onMoveToRoot) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Root (no parent)")
                }
                folders
                    .filter { it.id !in excludeIds }
                    .forEach { folder ->
                        TextButton(onClick = { onMoveToFolder(folder.id) }) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(folder.color),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name)
                        }
                    }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
