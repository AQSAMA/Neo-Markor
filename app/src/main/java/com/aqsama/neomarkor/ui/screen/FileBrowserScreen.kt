package com.aqsama.neomarkor.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileItem> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
) {
    val files = remember {
        listOf(
            FileItem("notes", "/notes", true, listOf(
                FileItem("meeting.md", "/notes/meeting.md", false),
                FileItem("ideas.md", "/notes/ideas.md", false),
                FileItem("journal", "/notes/journal", true, listOf(
                    FileItem("2026-02-20.md", "/notes/journal/2026-02-20.md", false),
                    FileItem("2026-02-19.md", "/notes/journal/2026-02-19.md", false),
                )),
            )),
            FileItem("projects", "/projects", true, listOf(
                FileItem("README.md", "/projects/README.md", false),
            )),
            FileItem("todo.txt", "/todo.txt", false),
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New file")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(files) { file ->
                FileTreeItem(
                    file = file,
                    depth = 0,
                    onOpenEditor = onOpenEditor
                )
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    file: FileItem,
    depth: Int,
    onOpenEditor: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (file.isDirectory) expanded = !expanded
                    else onOpenEditor(file.path)
                },
            shape = RoundedCornerShape(8.dp),
            color = if (file.isDirectory)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface,
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
                    onOpenEditor = onOpenEditor
                )
            }
        }
    }
}
