package com.aqsama.neomarkor.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqsama.neomarkor.domain.parser.FrontmatterParser
import com.aqsama.neomarkor.domain.parser.MarkdownHighlighter
import com.aqsama.neomarkor.presentation.viewmodel.EditorViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

enum class EditorMode { SOURCE, PREVIEW, READING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = koinViewModel { parametersOf(filePath) },
) {
    val content by viewModel.content.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var editorMode by remember { mutableStateOf(EditorMode.SOURCE) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Handle HTML export
    LaunchedEffect(Unit) {
        viewModel.exportHtml.collect { html ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_TEXT, html)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
            }
            context.startActivity(Intent.createChooser(intent, "Export as HTML"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (isSaving) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo / Redo
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { viewModel.saveNow() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export HTML") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.requestExportHtml()
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMoreMenu = false
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, content)
                                        putExtra(Intent.EXTRA_SUBJECT, fileName)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share"))
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            )
                        }
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
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                when (editorMode) {
                    EditorMode.SOURCE -> SourceEditor(
                        content = content,
                        onContentChange = { viewModel.onContentChanged(it) },
                        fileExtension = viewModel.getFileExtension(),
                    )
                    EditorMode.PREVIEW -> LivePreview(content = content)
                    EditorMode.READING -> ReadingMode(content = content)
                }
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
    fileExtension: String = "md",
) {
    val scrollState = rememberScrollState()
    val isMarkdown = fileExtension in setOf("md", "markdown")

    // Apply syntax highlighting for Markdown files
    val highlightedText = remember(content, isMarkdown) {
        if (isMarkdown && content.isNotEmpty()) {
            MarkdownHighlighter.highlight(content)
        } else null
    }

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
    // Strip frontmatter for clean reading
    val body = remember(content) { FrontmatterParser.stripFrontmatter(content) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        MarkdownText(content = body)
    }
}

@Composable
private fun MarkdownText(content: String) {
    val lines = content.split("\n")
    var inCodeBlock by remember { mutableStateOf(false) }
    val codeBlockLines = remember { mutableListOf<String>() }

    lines.forEach { line ->
        // Handle code fences
        if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
            if (inCodeBlock) {
                // End code block — render accumulated lines
                if (codeBlockLines.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        val codeScrollState = rememberScrollState()
                        Text(
                            text = codeBlockLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .horizontalScroll(codeScrollState)
                                .padding(12.dp),
                        )
                    }
                    codeBlockLines.clear()
                }
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            return@forEach
        }
        if (inCodeBlock) {
            codeBlockLines.add(line)
            return@forEach
        }

        when {
            // Horizontal rule
            line.trim().matches(Regex("""^[-*_]{3,}\s*$""")) -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
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
            line.startsWith("#### ") -> {
                Text(
                    text = line.removePrefix("#### "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // Blockquote
            line.trimStart().startsWith("> ") || line.trim() == ">" -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = line.trimStart().removePrefix(">").trim(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Task lists
            line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                val isDone = line.startsWith("- [x] ") || line.startsWith("- [X] ")
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
                        text = line.substring(6),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDone)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            // Unordered list
            line.startsWith("- ") || line.startsWith("* ") -> {
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
                        text = line.substring(2),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            // Wiki-links rendered inline
            line.contains("[[") -> {
                // Render with wiki-links highlighted
                Text(
                    text = line.replace(Regex("""\[\[([^\]]+?)(?:\|([^\]]+?))?\]\]""")) { match ->
                        val display = match.groupValues[2].ifEmpty { match.groupValues[1] }
                        "🔗$display"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
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
