package com.aqsama.neomarkor.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.presentation.viewmodel.DashboardViewModel
import com.aqsama.neomarkor.presentation.viewmodel.FolderNodeUi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private val FolderColorOptions = listOf(
    Color(0xFF6E6E6E),
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFF29B6F6),
    Color(0xFFD81B60),
    Color(0xFF6D4C41),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenFileBrowser: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val allNotes by viewModel.allNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var showManageFolders by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.newNoteEvent.collect { uri -> onOpenEditor(uri) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NeoMarkorDrawer(
                    folders = folders,
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    onOpenManageFolders = {
                        scope.launch { drawerState.close() }
                        showManageFolders = true
                    },
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "All notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                            }
                        },
                        actions = {
                            TextButton(onClick = onOpenFileBrowser) { Text("Export") }
                            IconButton(onClick = onOpenFileBrowser) { Icon(Icons.Default.Search, contentDescription = "Search") }
                            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { viewModel.createNewNote() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Create note")
                    }
                },
            ) { padding ->
                if (allNotes.isEmpty()) {
                    EmptyNotesState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    NotesContent(
                        allNotes = allNotes,
                        folders = folders,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onOpenNote = { onOpenEditor(it.uriString) },
                    )
                }
            }
        }

        if (showManageFolders) {
            ManageFoldersScreen(
                folders = folders,
                onBack = { showManageFolders = false },
                onCreateFolder = { name, parentUri, color -> viewModel.createFolder(name, parentUri, color) },
                onRename = { uri, newName -> viewModel.renameFolder(uri, newName) },
                onDelete = { uris -> viewModel.deleteFolders(uris) },
                onMove = { uris, target -> viewModel.moveFolders(uris, target) },
                onColor = { uris, color -> viewModel.setFolderColor(uris, color) },
            )
        }
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "No notes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the Add button to create a note.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotesContent(
    allNotes: List<FileNode>,
    folders: List<FolderNodeUi>,
    modifier: Modifier,
    onOpenNote: (FileNode) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Folders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${folders.countTotalFolders()} Folders, ${allNotes.size} notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "> Folders",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(folders) { folder ->
                    FolderCarouselCard(folder = folder)
                }
            }
        }
        items(allNotes) { note ->
            NotePreviewCard(note = note, onClick = { onOpenNote(note) })
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FolderCarouselCard(folder: FolderNodeUi) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .width(160.dp)
            .height(96.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(folder.colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${folder.noteCount}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NotePreviewCard(note: FileNode, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(note.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Size: ${note.sizeBytes} bytes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NeoMarkorDrawer(
    folders: List<FolderNodeUi>,
    onOpenSettings: () -> Unit,
    onOpenManageFolders: () -> Unit,
) {
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Navigation", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
            DrawerStaticItem(label = "All notes")
            DrawerStaticItem(label = "Trash")
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("Folders", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(folders) { folder ->
                    DrawerFolderItem(folder = folder, depth = 0, expandedState = expandedState)
                }
            }
            Button(
                onClick = onOpenManageFolders,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage folders")
            }
        }
    }
}

@Composable
private fun DrawerStaticItem(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DrawerFolderItem(
    folder: FolderNodeUi,
    depth: Int,
    expandedState: MutableMap<String, Boolean>,
) {
    val isExpanded = expandedState[folder.uri] ?: false
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .clickable {
                if (folder.children.isNotEmpty()) {
                    expandedState[folder.uri] = !isExpanded
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (folder.children.isNotEmpty()) {
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Icon(Icons.Default.Folder, contentDescription = null, tint = folder.colorArgb?.let(::Color) ?: Color(0xFFBDBDBD))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            folder.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Badge(containerColor = Color(0xFF353535)) {
            Text(folder.noteCount.toString(), color = Color(0xFFDDDDDD), fontSize = 11.sp)
        }
    }
    if (isExpanded) {
        folder.children.forEach { child ->
            DrawerFolderItem(folder = child, depth = depth + 1, expandedState = expandedState)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ManageFoldersScreen(
    folders: List<FolderNodeUi>,
    onBack: () -> Unit,
    onCreateFolder: (name: String, parentUri: String?, color: Int?) -> Unit,
    onRename: (uri: String, newName: String) -> Unit,
    onDelete: (Set<String>) -> Unit,
    onMove: (Set<String>, String?) -> Unit,
    onColor: (Set<String>, Int) -> Unit,
) {
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    val rows = remember { mutableStateListOf<ManageFolderRow>() }
    var editMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showCreateDialogForParent by remember { mutableStateOf<String?>(null) }
    var showColorDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ManageFolderRow?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var dragBadgeCount by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(density) { 64.dp.toPx() }

    LaunchedEffect(folders) {
        rows.clear()
        rows.addAll(flattenFolders(folders, expandedMap))
    }

    if (showCreateDialogForParent != null) {
        FolderCreateDialog(
            onDismiss = { showCreateDialogForParent = null },
            onAdd = { name, color ->
                val parentUri = showCreateDialogForParent?.ifBlank { null }
                onCreateFolder(name, parentUri, color)
                showCreateDialogForParent = null
            },
        )
    }

    if (renameTarget != null) {
        var input by remember(renameTarget) { mutableStateOf(renameTarget!!.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = { OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (input.isNotBlank()) {
                        onRename(renameTarget!!.uri, input.trim())
                        renameTarget = null
                        selected.clear()
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move selected") },
            text = {
                Column {
                    TextButton(onClick = {
                        onMove(selected.toSet(), null)
                        selected.clear()
                        showMoveDialog = false
                    }) { Text("Move to root") }
                    folders.flatten().forEach { folder ->
                        TextButton(onClick = {
                            onMove(selected.toSet(), folder.uri)
                            selected.clear()
                            showMoveDialog = false
                        }) {
                            Text(folder.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } },
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Folder color") },
            text = {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    FolderColorOptions.forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    onColor(selected.toSet(), color.value.toInt())
                                    showColorDialog = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showColorDialog = false }) { Text("Close") } },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    if (editMode) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color(0xFFB0B0B0))
                            Text("All", color = Color(0xFFBDBDBD), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${selected.size} selected", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Text("Manage folders", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        editMode = !editMode
                        if (!editMode) selected.clear()
                    }) {
                        Text(if (editMode) "Done" else "Edit")
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(rows, key = { _, row -> row.uri }) { index, row ->
                            val selectedThis = row.uri in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .pointerInput(editMode, selectedThis, row.uri) {
                                        if (editMode && selectedThis) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    dragBadgeCount = selected.size.coerceAtLeast(1)
                                                    dragX = it.x
                                                    dragY = it.y
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragX += amount.x
                                                    dragY += amount.y
                                                    val step = (dragY / rowHeightPx).roundToInt()
                                                    if (step != 0) {
                                                        val target = (index + step).coerceIn(0, rows.lastIndex)
                                                        if (target != index) {
                                                            rows.move(index, target)
                                                            dragY = 0f
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    dragBadgeCount = 0
                                                    dragX = 0f
                                                    dragY = 0f
                                                },
                                                onDragCancel = {
                                                    dragBadgeCount = 0
                                                    dragX = 0f
                                                    dragY = 0f
                                                },
                                            )
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            if (editMode) {
                                                if (selectedThis) selected.remove(row.uri) else selected.add(row.uri)
                                            } else if (row.hasChildren) {
                                                expandedMap[row.uri] = !(expandedMap[row.uri] ?: false)
                                                rows.clear()
                                                rows.addAll(flattenFolders(folders, expandedMap))
                                            }
                                        },
                                        onLongClick = {
                                            if (!editMode) {
                                                editMode = true
                                                if (!selectedThis) selected.add(row.uri)
                                            }
                                        },
                                    )
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (editMode) {
                                    Icon(
                                        imageVector = if (selectedThis) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (selectedThis) Color(0xFF3A82FF) else Color(0xFFABABAB),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Spacer(modifier = Modifier.width((row.depth * 18).dp))
                                if (row.hasChildren) {
                                    Icon(
                                        imageVector = if (expandedMap[row.uri] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFFA9A9A9),
                                    )
                                }
                                Icon(Icons.Default.Folder, contentDescription = null, tint = row.colorArgb?.let(::Color) ?: Color(0xFF969696))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(row.name, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (row.noteCount > 0) {
                                    Text(row.noteCount.toString(), color = Color(0xFFA9A9A9), modifier = Modifier.padding(end = 8.dp))
                                }
                                if (!editMode) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "Reorder", tint = Color(0xFF8A8A8A))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateDialogForParent = "" }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F9D58))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Create folder", color = Color(0xFF7F7F7F), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                if (editMode) {
                    FolderEditActionBar(
                        onMove = { if (selected.isNotEmpty()) showMoveDialog = true },
                        onCreateSub = {
                            val parent = selected.firstOrNull() ?: return@FolderEditActionBar
                            showCreateDialogForParent = parent
                        },
                        onColor = { if (selected.isNotEmpty()) showColorDialog = true },
                        onRename = {
                            val row = rows.firstOrNull { it.uri == selected.firstOrNull() } ?: return@FolderEditActionBar
                            renameTarget = row
                        },
                        onDelete = {
                            val toDelete = selected.toSet()
                            onDelete(toDelete)
                            selected.clear()
                        },
                    )
                }
            }

            if (dragBadgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                        .size(98.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFA8A8AA).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF909090), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF6B6B6B), modifier = Modifier.size(44.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-6).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C6DDE)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dragBadgeCount.toString(), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderEditActionBar(
    onMove: () -> Unit,
    onCreateSub: () -> Unit,
    onColor: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ActionItem(icon = Icons.Default.DriveFileMove, label = "Move", onClick = onMove)
            ActionItem(icon = Icons.Default.Add, label = "Create sub...", onClick = onCreateSub)
            ActionItem(icon = Icons.Default.Palette, label = "Folder color", onClick = onColor)
            ActionItem(icon = Icons.Default.Edit, label = "Rename", onClick = onRename)
            ActionItem(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFFE8E8E8))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color(0xFFE8E8E8), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FolderCreateDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var input by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(FolderColorOptions.first().value.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    label = { Text("Folder name") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    FolderColorOptions.forEach { swatch ->
                        val colorInt = swatch.value.toInt()
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .clickable { selectedColor = colorInt },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == colorInt) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onAdd(input.trim(), selectedColor)
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class ManageFolderRow(
    val uri: String,
    val parentUri: String?,
    val name: String,
    val noteCount: Int,
    val depth: Int,
    val hasChildren: Boolean,
    val colorArgb: Int?,
)

private fun flattenFolders(
    folders: List<FolderNodeUi>,
    expandedMap: Map<String, Boolean>,
    depth: Int = 0,
): List<ManageFolderRow> = buildList {
    folders.forEach { folder ->
        add(
            ManageFolderRow(
                uri = folder.uri,
                parentUri = folder.parentUri,
                name = folder.name,
                noteCount = folder.noteCount,
                depth = depth,
                hasChildren = folder.children.isNotEmpty(),
                colorArgb = folder.colorArgb,
            ),
        )
        if (expandedMap[folder.uri] == true) {
            addAll(flattenFolders(folder.children, expandedMap, depth + 1))
        }
    }
}

private fun List<FolderNodeUi>.flatten(): List<FolderNodeUi> = buildList {
    fun addNode(node: FolderNodeUi) {
        add(node)
        node.children.forEach(::addNode)
    }
    this@flatten.forEach(::addNode)
}

private fun List<FolderNodeUi>.countTotalFolders(): Int = flatten().size

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(to, item)
}
