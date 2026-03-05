package com.gamerx.manager.feature.script.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.manager.feature.script.data.ScriptRepository
import com.gamerx.manager.feature.script.model.ScriptManifest
import com.gamerx.manager.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Script Editor Screen - File Browser & Code Editor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var manifest by remember { mutableStateOf<ScriptManifest?>(null) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var isModified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<File?>(null) }
    var showSetEntryPointDialog by remember { mutableStateOf(false) }
    var isFilesPanelExpanded by remember { mutableStateOf(true) }
    
    // Load script data
    LaunchedEffect(scriptId) {
        withContext(Dispatchers.IO) {
            manifest = ScriptRepository.getScript(scriptId)
            val scriptDir = ScriptRepository.getScriptDirectory(scriptId)
            if (scriptDir?.exists() == true) {
                files = scriptDir.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedBy { it.name }
                    ?: emptyList()
            }
            isLoading = false
        }
    }
    
    // Load file content when selected
    LaunchedEffect(selectedFile) {
        selectedFile?.let { file ->
            withContext(Dispatchers.IO) {
                fileContent = try {
                    file.readText()
                } catch (e: Exception) {
                    "// Error reading file: ${e.message}"
                }
            }
            isModified = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            manifest?.identity?.name ?: "Script Editor",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (selectedFile != null) {
                            Text(
                                selectedFile!!.name + if (isModified) " •" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isModified) ThemeManager.accentColor.value else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Save Button
                    if (selectedFile != null && isModified) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                selectedFile?.writeText(fileContent)
                                isModified = false
                            }
                        }) {
                            Icon(Icons.Default.Save, "Save", tint = ThemeManager.accentColor.value)
                        }
                    }
                    
                    // Create File
                    IconButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Default.NoteAdd, "New File", tint = Color.White)
                    }
                    
                    // Set Entry Point
                    IconButton(onClick = { showSetEntryPointDialog = true }) {
                        Icon(Icons.Default.PlayCircle, "Set Entry Point", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ThemeManager.accentColor.value)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Collapsible File Tree Panel
                if (isFilesPanelExpanded) {
                    FileTreePanel(
                        files = files,
                        selectedFile = selectedFile,
                        entryPoint = manifest?.execution?.entryPoint,
                        onFileSelect = { selectedFile = it },
                        onFileDelete = { showDeleteConfirmDialog = it },
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF151515))
                    )
                }
                
                // Toggle + Divider Column
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF101010)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toggle files button
                    IconButton(
                        onClick = { isFilesPanelExpanded = !isFilesPanelExpanded },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            if (isFilesPanelExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                            contentDescription = if (isFilesPanelExpanded) "Hide Files" else "Show Files",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Code Editor Panel
                if (selectedFile != null) {
                    CodeEditorPanel(
                        content = fileContent,
                        onContentChange = {
                            fileContent = it
                            isModified = true
                        },
                        fileName = selectedFile!!.name,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    // No file selected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Code,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select a file to edit",
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        // Create File Dialog
        if (showCreateFileDialog) {
            CreateFileDialog(
                onDismiss = { showCreateFileDialog = false },
                onCreate = { fileName ->
                    scope.launch(Dispatchers.IO) {
                        val scriptDir = ScriptRepository.getScriptDirectory(scriptId)
                        if (scriptDir != null) {
                            val newFile = File(scriptDir, fileName)
                            newFile.createNewFile()
                            
                            // Add default content based on extension
                            when (newFile.extension.lowercase()) {
                                "sh" -> newFile.writeText("#!/system/bin/sh\n# Script logic here\n")
                                "py" -> newFile.writeText("#!/usr/bin/env python3\n# Script logic here\n")
                                "json" -> newFile.writeText("{}\n")
                            }
                            
                            // Refresh file list
                            files = scriptDir.listFiles()
                                ?.filter { it.isFile }
                                ?.sortedBy { it.name }
                                ?: emptyList()
                            
                            selectedFile = newFile
                        }
                    }
                    showCreateFileDialog = false
                }
            )
        }
        
        // Delete File Confirmation
        showDeleteConfirmDialog?.let { file ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Delete File?", color = Color.White) },
                text = { Text("Delete '${file.name}'?", color = Color.Gray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                file.delete()
                                val scriptDir = ScriptRepository.getScriptDirectory(scriptId)
                                files = scriptDir?.listFiles()
                                    ?.filter { it.isFile }
                                    ?.sortedBy { it.name }
                                    ?: emptyList()
                                if (selectedFile == file) {
                                    selectedFile = null
                                    fileContent = ""
                                }
                            }
                            showDeleteConfirmDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("DELETE") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
        
        // Set Entry Point Dialog
        if (showSetEntryPointDialog && manifest != null) {
            SetEntryPointDialog(
                files = files,
                currentEntryPoint = manifest?.execution?.entryPoint,
                onDismiss = { showSetEntryPointDialog = false },
                onSelect = { fileName ->
                    scope.launch(Dispatchers.IO) {
                        val updated = manifest!!.copy(
                            execution = manifest!!.execution.copy(entryPoint = fileName)
                        )
                        ScriptRepository.updateManifest(updated)
                        manifest = updated
                    }
                    showSetEntryPointDialog = false
                }
            )
        }
    }
}

// ==================== FILE TREE PANEL ====================

@Composable
fun FileTreePanel(
    files: List<File>,
    selectedFile: File?,
    entryPoint: String?,
    onFileSelect: (File) -> Unit,
    onFileDelete: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                "FILES",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(files) { file ->
            val isSelected = file == selectedFile
            val isEntryPoint = file.name == entryPoint
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onFileSelect(file) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getFileIcon(file.extension),
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isEntryPoint) ThemeManager.accentColor.value else getFileIconColor(file.extension)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEntryPoint) ThemeManager.accentColor.value else Color.White,
                    maxLines = 1
                )
                
                if (isEntryPoint) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        "Entry Point",
                        modifier = Modifier.size(12.dp),
                        tint = ThemeManager.accentColor.value
                    )
                }
            }
        }
    }
}

@Composable
fun getFileIcon(extension: String) = when (extension.lowercase()) {
    "sh", "bash" -> Icons.Default.Terminal
    "py" -> Icons.Default.Code
    "json" -> Icons.Default.DataObject
    "txt", "md" -> Icons.Default.Description
    else -> Icons.Default.InsertDriveFile
}

fun getFileIconColor(extension: String) = when (extension.lowercase()) {
    "sh", "bash" -> Color(0xFF4CAF50)
    "py" -> Color(0xFF3776AB)
    "json" -> Color(0xFFFFA000)
    else -> Color.Gray
}

// ==================== CODE EDITOR PANEL ====================

@Composable
fun CodeEditorPanel(
    content: String,
    onContentChange: (String) -> Unit,
    fileName: String,
    modifier: Modifier = Modifier
) {
    // Single shared scroll state for synchronized scrolling
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .windowInsetsPadding(WindowInsets.ime)  // Keyboard insets
            .windowInsetsPadding(WindowInsets.navigationBars)  // All nav bar types
    ) {
        // File info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181818))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                fileName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                "${content.lines().size} lines",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        // Editor with synchronized scrolling
        Row(modifier = Modifier.weight(1f)) {
            val lines = content.lines()
            
            // Line numbers - uses same scroll state, disabled interaction
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0A))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            "${index + 1}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp  // Match code line height
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            
            // Code area - scrollable, user can scroll here
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFE0E0E0),
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(ThemeManager.accentColor.value),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)  // Same scroll state!
                    .padding(8.dp)
            )
        }
    }
}

// ==================== DIALOGS ====================

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("sh") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Create New File", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GamerXTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = "File Name",
                    placeholder = "logic"
                )
                
                Text("Extension", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sh", "py", "json", "txt").forEach { ext ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (ext == extension) ThemeManager.accentColor.value.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { extension = ext }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                ".$ext",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ext == extension) ThemeManager.accentColor.value else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (fileName.isNotBlank()) {
                        onCreate("$fileName.$extension")
                    }
                },
                enabled = fileName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun SetEntryPointDialog(
    files: List<File>,
    currentEntryPoint: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val executableFiles = files.filter { 
        it.extension.lowercase() in listOf("sh", "py", "bash")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Set Entry Point", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select the main script file to execute:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                if (executableFiles.isEmpty()) {
                    Text(
                        "No executable files (.sh, .py) found",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    executableFiles.forEach { file ->
                        val isSelected = file.name == currentEntryPoint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { onSelect(file.name) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                getFileIcon(file.extension),
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = getFileIconColor(file.extension)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(file.name, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = ThemeManager.accentColor.value
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) }
        }
    )
}
