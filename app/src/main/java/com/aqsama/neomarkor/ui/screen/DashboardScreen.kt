package com.aqsama.neomarkor.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onOpenFileBrowser: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val hasDirectory by viewModel.hasDirectory.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val pinnedUris by viewModel.pinnedNoteUris.collectAsState()

    // Navigate to the editor whenever a new note is successfully created
    LaunchedEffect(Unit) {
        viewModel.newNoteEvent.collect { uri -> onOpenEditor(uri) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NeoMarkorDrawer(
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
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Neo-Markor",
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createNewNote() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Quick Note") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
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
    onCloseDraw: () -> Unit,
    onOpenFileBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDailyNote: () -> Unit,
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
                IconButton(onClick = onCloseDraw) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Dashboard") },
                selected = true,
                onClick = onCloseDraw
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                label = { Text("File Browser") },
                selected = false,
                onClick = onOpenFileBrowser
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Today, contentDescription = null) },
                label = { Text("Daily Note") },
                selected = false,
                onClick = onOpenDailyNote
            )
            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider()
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = onOpenSettings
            )
        }
    }
}
