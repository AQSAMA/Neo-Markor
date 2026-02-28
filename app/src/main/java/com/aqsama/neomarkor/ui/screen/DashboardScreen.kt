package com.aqsama.neomarkor.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.model.Folder
import com.aqsama.neomarkor.presentation.viewmodel.DashboardViewModel
import com.aqsama.neomarkor.presentation.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onOpenFileBrowser: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManageFolders: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    folderViewModel: FolderViewModel = koinViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val hasDirectory by viewModel.hasDirectory.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val pinnedUris by viewModel.pinnedNoteUris.collectAsState()
    val folders by folderViewModel.folders.collectAsState()

    // Navigate to the editor whenever a new note is successfully created
    LaunchedEffect(Unit) {
        viewModel.newNoteEvent.collect { uri -> onOpenEditor(uri) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NeoMarkorDrawer(
                folders = folders,
                folderViewModel = folderViewModel,
                onCloseDraw = { scope.launch { drawerState.close() } },
                onOpenFileBrowser = {
                    scope.launch { drawerState.close() }
                    onOpenFileBrowser()
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onOpenDailyNote = {
                    scope.launch { drawerState.close() }
                    viewModel.openDailyNote()
                },
                onOpenManageFolders = {
                    scope.launch { drawerState.close() }
                    onOpenManageFolders()
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
                            fontWeight = FontWeight.Bold
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
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Create new note")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasDirectory) {
                    item {
                        NoWorkspaceCard(onOpenFileBrowser = onOpenFileBrowser)
                    }
                } else if (recentFiles.isEmpty() && pinnedNotes.isEmpty()) {
                    // Empty state: "No notes" prompt
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.7f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the button below to create a note.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    // ── Pinned Notes ────────────────────────────────────
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(pinnedNotes, key = { "pinned_${it.uriString}" }) { file ->
                            RecentFileCard(
                                file = file,
                                isPinned = true,
                                onClick = { onOpenEditor(file.uriString) },
                                onLongClick = { viewModel.togglePin(file.uriString) },
                            )
                        }
                    }

                    // ── Recent Notes ────────────────────────────────────
                    item {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    if (recentFiles.isEmpty()) {
                        item {
                            Text(
                                text = "No files found in your workspace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(recentFiles, key = { "recent_${it.uriString}" }) { file ->
                            RecentFileCard(
                                file = file,
                                isPinned = file.uriString in pinnedUris,
                                onClick = { onOpenEditor(file.uriString) },
                                onLongClick = { viewModel.togglePin(file.uriString) },
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun NoWorkspaceCard(onOpenFileBrowser: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFileBrowser),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No workspace selected",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tap to open File Browser and choose a directory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.name.endsWith(".md")) Icons.Default.Description else Icons.Default.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NeoMarkorDrawer(
    folders: List<Folder>,
    folderViewModel: FolderViewModel,
    onCloseDraw: () -> Unit,
    onOpenFileBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDailyNote: () -> Unit,
    onOpenManageFolders: () -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Neo-Markor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // All notes
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
                label = { Text("All notes") },
                selected = true,
                onClick = onCloseDraw
            )

            // Trash
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                label = { Text("Trash") },
                selected = false,
                onClick = onCloseDraw
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Folders section header
            Text(
                text = "Folders",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            )

            // Folder list in drawer with expand/collapse
            val rootFolders = folderViewModel.getRootFolders(folders)
            rootFolders.forEach { folder ->
                DrawerFolderItem(
                    folder = folder,
                    allFolders = folders,
                    depth = 0,
                    folderViewModel = folderViewModel,
                    onClick = onCloseDraw,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Manage Folders button
            Button(
                onClick = onOpenManageFolders,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Folders")
            }
        }
    }
}

@Composable
private fun DrawerFolderItem(
    folder: Folder,
    allFolders: List<Folder>,
    depth: Int,
    folderViewModel: FolderViewModel,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val children = folderViewModel.getChildFolders(folder.id, allFolders)
    val hasChildren = children.isNotEmpty()
    val noteCount = folderViewModel.countNotesInSubtree(folder.id, allFolders)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start = (16 + depth * 16).dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color(folder.color),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                folder.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            // Note count badge
            Badge(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text("$noteCount")
            }
        }

        // Expanded children
        AnimatedVisibility(visible = expanded) {
            Column {
                children.forEach { child ->
                    DrawerFolderItem(
                        folder = child,
                        allFolders = allFolders,
                        depth = depth + 1,
                        folderViewModel = folderViewModel,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}
