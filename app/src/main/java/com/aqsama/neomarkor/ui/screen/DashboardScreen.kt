package com.aqsama.neomarkor.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.model.FolderMetadata
import com.aqsama.neomarkor.presentation.viewmodel.DashboardViewModel
import com.aqsama.neomarkor.presentation.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Which section of the drawer is currently selected. */
private sealed class DrawerSelection {
    object AllNotes : DrawerSelection()
    object Trash : DrawerSelection()
    data class Folder(val id: String) : DrawerSelection()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onOpenFileBrowser: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onManageFolders: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    folderViewModel: FolderViewModel = koinViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val hasDirectory by viewModel.hasDirectory.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val pinnedUris by viewModel.pinnedNoteUris.collectAsState()

    val foldersWithCounts by folderViewModel.foldersWithCounts.collectAsState()
    val allNotes by folderViewModel.allNotes.collectAsState()
    val trashedNotes by folderViewModel.trashedNotes.collectAsState()
    val folderUiState by folderViewModel.uiState.collectAsState()

    var drawerSelection by remember { mutableStateOf<DrawerSelection>(DrawerSelection.AllNotes) }

    // Navigate to the editor whenever a new note is successfully created
    LaunchedEffect(Unit) {
        viewModel.newNoteEvent.collect { uri -> onOpenEditor(uri) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NeoMarkorDrawer(
                foldersWithCounts = foldersWithCounts,
                expandedFolderIds = folderUiState.expandedIds,
                selection = drawerSelection,
                onSelect = { sel ->
                    drawerSelection = sel
                    scope.launch { drawerState.close() }
                },
                onToggleExpand = { folderViewModel.toggleExpanded(it) },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onManageFolders = {
                    scope.launch { drawerState.close() }
                    onManageFolders()
                },
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (val sel = drawerSelection) {
                                is DrawerSelection.AllNotes -> "All notes"
                                is DrawerSelection.Trash -> "Trash"
                                is DrawerSelection.Folder -> {
                                    foldersWithCounts.find { it.first.id == sel.id }?.first?.name
                                        ?: "Folder"
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onOpenFileBrowser() }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "File browser")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.createNewNote() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New note")
                }
            }
        ) { padding ->
            when (val sel = drawerSelection) {
                is DrawerSelection.AllNotes -> AllNotesContent(
                    padding = padding,
                    hasDirectory = hasDirectory,
                    allNotes = allNotes,
                    pinnedNotes = pinnedNotes,
                    pinnedUris = pinnedUris,
                    recentFiles = recentFiles,
                    foldersWithCounts = foldersWithCounts,
                    noteFolderMap = folderUiState.noteFolderMap,
                    onOpenFileBrowser = onOpenFileBrowser,
                    onOpenEditor = onOpenEditor,
                    onTogglePin = { viewModel.togglePin(it) },
                    onSelectFolder = { drawerSelection = DrawerSelection.Folder(it) },
                )
                is DrawerSelection.Trash -> TrashContent(
                    padding = padding,
                    trashedNotes = trashedNotes,
                    onRestore = { folderViewModel.restoreNote(it) },
                    onEmptyTrash = { folderViewModel.emptyTrash() },
                )
                is DrawerSelection.Folder -> {
                    val folder = foldersWithCounts.find { it.first.id == sel.id }?.first
                    FolderNotesContent(
                        padding = padding,
                        folder = folder,
                        allNotes = allNotes,
                        noteFolderMap = folderUiState.noteFolderMap,
                        folderId = sel.id,
                        onOpenEditor = onOpenEditor,
                        onTogglePin = { viewModel.togglePin(it) },
                        pinnedUris = pinnedUris,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllNotesContent(
    padding: PaddingValues,
    hasDirectory: Boolean,
    allNotes: List<FileNode>,
    pinnedNotes: List<FileNode>,
    pinnedUris: Set<String>,
    recentFiles: List<FileNode>,
    foldersWithCounts: List<Pair<FolderMetadata, Int>>,
    noteFolderMap: Map<String, String>,
    onOpenFileBrowser: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!hasDirectory) {
            item { NoWorkspaceCard(onOpenFileBrowser = onOpenFileBrowser) }
        } else {
            // Horizontal folder carousel (if folders exist)
            if (foldersWithCounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                item {
                    FolderCarousel(
                        folders = foldersWithCounts.filter { it.first.parentId == null },
                        onSelectFolder = onSelectFolder,
                    )
                }
            }

            // Pinned notes
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Text(
                        text = "Pinned",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(pinnedNotes, key = { "pinned_${it.uriString}" }) { file ->
                    RecentFileCard(
                        file = file,
                        isPinned = true,
                        onClick = { onOpenEditor(file.uriString) },
                        onLongClick = { onTogglePin(file.uriString) },
                    )
                }
            }

            // Recent / All notes
            item {
                Text(
                    text = if (allNotes.isEmpty()) "" else "Recent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            if (allNotes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "No notes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Tap the Edit button to create a note.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(allNotes.take(20), key = { "note_${it.uriString}" }) { file ->
                    RecentFileCard(
                        file = file,
                        isPinned = file.uriString in pinnedUris,
                        onClick = { onOpenEditor(file.uriString) },
                        onLongClick = { onTogglePin(file.uriString) },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FolderCarousel(
    folders: List<Pair<FolderMetadata, Int>>,
    onSelectFolder: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(folders, key = { it.first.id }) { (folder, count) ->
            FolderCarouselCard(
                folder = folder,
                noteCount = count,
                onClick = { onSelectFolder(folder.id) },
            )
        }
    }
}

@Composable
private fun FolderCarouselCard(
    folder: FolderMetadata,
    noteCount: Int,
    onClick: () -> Unit,
) {
    val folderColor = Color(folder.colorArgb)
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Color accent strip at top right
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(folderColor),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = noteCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderNotesContent(
    padding: PaddingValues,
    folder: FolderMetadata?,
    allNotes: List<FileNode>,
    noteFolderMap: Map<String, String>,
    folderId: String,
    onOpenEditor: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    pinnedUris: Set<String>,
) {
    val folderNotes = remember(allNotes, noteFolderMap, folderId) {
        allNotes.filter { noteFolderMap[it.uriString] == folderId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (folderNotes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "No notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Tap the Edit button to create a note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(folderNotes, key = { "folder_note_${it.uriString}" }) { file ->
                RecentFileCard(
                    file = file,
                    isPinned = file.uriString in pinnedUris,
                    onClick = { onOpenEditor(file.uriString) },
                    onLongClick = { onTogglePin(file.uriString) },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TrashContent(
    padding: PaddingValues,
    trashedNotes: List<FileNode>,
    onRestore: (String) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (trashedNotes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEmptyTrash) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Empty Trash")
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (trashedNotes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("Trash is empty", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(trashedNotes, key = { "trash_${it.uriString}" }) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = file.name, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(onClick = { onRestore(file.uriString) }) {
                                Text("Restore")
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NoWorkspaceCard(onOpenFileBrowser: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFileBrowser),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FolderOpen, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("No workspace selected", style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("Tap to open File Browser and choose a directory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFileCard(
    file: FileNode,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (file.name.endsWith(".md")) Icons.Default.Description else Icons.Default.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isPinned) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Navigation Drawer ────────────────────────────────────────────────────────

@Composable
private fun NeoMarkorDrawer(
    foldersWithCounts: List<Pair<FolderMetadata, Int>>,
    expandedFolderIds: Set<String>,
    selection: DrawerSelection,
    onSelect: (DrawerSelection) -> Unit,
    onToggleExpand: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onManageFolders: () -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Header row with settings icon
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Neo-Markor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        label = { Text("All notes") },
                        selected = selection is DrawerSelection.AllNotes,
                        onClick = { onSelect(DrawerSelection.AllNotes) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        label = { Text("Trash") },
                        selected = selection is DrawerSelection.Trash,
                        onClick = { onSelect(DrawerSelection.Trash) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item {
                    Text(
                        "Folders",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                // Root-level folders
                items(foldersWithCounts.filter { it.first.parentId == null }
                    .sortedWith(compareBy({ it.first.orderIndex }, { it.first.name })),
                    key = { it.first.id }
                ) { (folder, count) ->
                    DrawerFolderItem(
                        folder = folder,
                        noteCount = count,
                        depth = 0,
                        isSelected = selection is DrawerSelection.Folder && selection.id == folder.id,
                        isExpanded = folder.id in expandedFolderIds,
                        hasChildren = foldersWithCounts.any { it.first.parentId == folder.id },
                        onSelect = { onSelect(DrawerSelection.Folder(folder.id)) },
                        onToggleExpand = { onToggleExpand(folder.id) },
                    )
                    // Children when expanded
                    if (folder.id in expandedFolderIds) {
                        foldersWithCounts
                            .filter { it.first.parentId == folder.id }
                            .sortedWith(compareBy({ it.first.orderIndex }, { it.first.name }))
                            .forEach { (child, childCount) ->
                                DrawerFolderItem(
                                    folder = child,
                                    noteCount = childCount,
                                    depth = 1,
                                    isSelected = selection is DrawerSelection.Folder && selection.id == child.id,
                                    isExpanded = child.id in expandedFolderIds,
                                    hasChildren = foldersWithCounts.any { it.first.parentId == child.id },
                                    onSelect = { onSelect(DrawerSelection.Folder(child.id)) },
                                    onToggleExpand = { onToggleExpand(child.id) },
                                )
                            }
                    }
                }

                item {
                    // "Manage Folders" pill button
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onManageFolders),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Manage Folders",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerFolderItem(
    folder: FolderMetadata,
    noteCount: Int,
    depth: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    hasChildren: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
) {
    val folderColor = Color(folder.colorArgb)
    NavigationDrawerItem(
        icon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasChildren) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clickable { onToggleExpand() },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Icon(Icons.Default.Folder, contentDescription = null, tint = folderColor)
            }
        },
        label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        badge = {
            if (noteCount > 0) {
                Badge { Text(noteCount.toString()) }
            }
        },
        selected = isSelected,
        onClick = onSelect,
        modifier = Modifier.padding(start = (8 + depth * 16).dp, end = 8.dp),
    )
}
