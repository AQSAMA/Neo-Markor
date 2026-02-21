package com.aqsama.neomarkor.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EditorMode { SOURCE, PREVIEW, READING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
) {
    val isNewNote = filePath == "new_note"
    val fileName = when {
        isNewNote -> "New Note"
        filePath.startsWith("content://") ->
            Uri.parse(filePath).lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast(":")
                ?: "File"
        else -> filePath.substringAfterLast("/")
    }

    var editorMode by remember { mutableStateOf(EditorMode.SOURCE) }
    var content by remember {
        mutableStateOf(
            if (isNewNote) "# New Note\n\n"
            else "# $fileName\n\nStart writing your markdown here...\n\n## Section\n\nSome content with **bold** and *italic* text.\n\n- Item 1\n- Item 2\n- [ ] Task item\n"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        },
        bottomBar = {
            EditorModeBar(
                currentMode = editorMode,
                onModeChange = { editorMode = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (editorMode) {
                EditorMode.SOURCE -> SourceEditor(
                    content = content,
                    onContentChange = { content = it }
                )
                EditorMode.PREVIEW -> LivePreview(content = content)
                EditorMode.READING -> ReadingMode(content = content)
            }
        }
    }
}

@Composable
private fun EditorModeBar(
    currentMode: EditorMode,
    onModeChange: (EditorMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            EditorMode.entries.forEach { mode ->
                val selected = mode == currentMode
                FilterChip(
                    selected = selected,
                    onClick = { onModeChange(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                EditorMode.SOURCE -> "Source"
                                EditorMode.PREVIEW -> "Preview"
                                EditorMode.READING -> "Reading"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                EditorMode.SOURCE -> Icons.Default.Code
                                EditorMode.PREVIEW -> Icons.Default.Splitscreen
                                EditorMode.READING -> Icons.Default.MenuBook
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceEditor(
    content: String,
    onContentChange: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (content.isEmpty()) {
                Text(
                    text = "Start writing...",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun LivePreview(content: String) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        MarkdownText(content = content)
    }
}

@Composable
private fun ReadingMode(content: String) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        MarkdownText(content = content)
    }
}

@Composable
private fun MarkdownText(content: String) {
    val lines = content.split("\n")
    lines.forEach { line ->
        when {
            line.startsWith("# ") -> {
                Text(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            line.startsWith("## ") -> {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            line.startsWith("### ") -> {
                Text(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                val isDone = line.startsWith("- [x] ")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = line.removePrefix(if (isDone) "- [x] " else "- [ ] "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDone)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            line.startsWith("- ") -> {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Text(
                        text = line.removePrefix("- "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            line.isBlank() -> {
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
