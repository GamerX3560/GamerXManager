package com.gamerx.manager.feature.script.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamerx.manager.feature.script.model.*
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptScreen(
    viewModel: ScriptViewModel = viewModel(),
    onNavigateToEditor: (String) -> Unit = {}
) {
    val scripts by viewModel.scripts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val tileBindings by viewModel.tileBindings.collectAsState()
    
    var selectedScript by remember { mutableStateOf<ScriptManifest?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Context menu states
    var contextMenuScript by remember { mutableStateOf<ScriptManifest?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showQsBindingDialog by remember { mutableStateOf(false) }
    var showRepoDialog by remember { mutableStateOf(false) }

    // File Picker for Import
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.importScript(it) }
        }
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Script",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Applets",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ThemeManager.accentColor.value,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White)
                    }
                    
                    IconButton(
                        onClick = { launcher.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Import", tint = Color.White)
                    }
                    
                    IconButton(
                        onClick = { showRepoDialog = true },
                        modifier = Modifier.background(ThemeManager.accentColor.value.copy(0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = "Add Repo", tint = ThemeManager.accentColor.value)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemeManager.accentColor.value)
                    }
                }
                scripts.isEmpty() -> {
                    EmptyState(
                        onCreateClick = { showCreateDialog = true },
                        onImportClick = { launcher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(scripts) { script ->
                            ScriptCardWithContextMenu(
                                script = script,
                                binding = tileBindings.getBindingByScriptId(script.id),
                                onClick = { 
                                    selectedScript = script
                                    showSheet = true
                                    viewModel.clearExecutionOutput()
                                },
                                onEditClick = { onNavigateToEditor(script.id) },
                                onPropertiesClick = {
                                    contextMenuScript = script
                                    showPropertiesDialog = true
                                },
                                onQsBindingClick = {
                                    contextMenuScript = script
                                    showQsBindingDialog = true
                                },
                                onDeleteClick = {
                                    contextMenuScript = script
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom Sheet for Script Config
        if (showSheet && selectedScript != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White,
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                ScriptConfigContent(
                    script = selectedScript!!,
                    viewModel = viewModel,
                    onDismiss = { showSheet = false },
                    onEditClick = { onNavigateToEditor(selectedScript!!.id) }
                )
            }
        }
        
        // Create Dialog
        if (showCreateDialog) {
            CreateScriptWizardDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc ->
                    viewModel.createScript(name, desc, "empty")
                    showCreateDialog = false
                },
                onCreateAndEdit = { name, desc ->
                    val scriptId = viewModel.createScriptAndGetId(name, desc)
                    showCreateDialog = false
                    if (scriptId != null) {
                        onNavigateToEditor(scriptId)
                    }
                }
            )
        }
        
        // Delete Confirmation
        if (showDeleteDialog && contextMenuScript != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color(0xFF1E1E1E),
                icon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                title = { Text("Delete Script?", color = Color.White) },
                text = { 
                    Text(
                        "This will permanently delete '${contextMenuScript!!.identity.name}' and remove any tile bindings.",
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteScript(contextMenuScript!!.id)
                            showDeleteDialog = false
                            contextMenuScript = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("DELETE") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
        
        // Properties Dialog
        if (showPropertiesDialog && contextMenuScript != null) {
            ScriptPropertiesDialog(
                script = contextMenuScript!!,
                onDismiss = { showPropertiesDialog = false }
            )
        }
        
        // QS Binding Dialog
        if (showQsBindingDialog && contextMenuScript != null) {
            QsBindingDialog(
                script = contextMenuScript!!,
                currentBinding = tileBindings.getBindingByScriptId(contextMenuScript!!.id),
                allBindings = tileBindings,
                onDismiss = { showQsBindingDialog = false },
                onBind = { slot, iconName, mode, quickSelectOptions -> 
                    viewModel.bindScriptToTile(slot, contextMenuScript!!, iconName, mode, quickSelectOptions)
                    showQsBindingDialog = false
                },
                onUnbind = { slot ->
                    viewModel.unbindTile(slot)
                    showQsBindingDialog = false
                }
            )
        }
        
        // Repo Import Dialog
        if (showRepoDialog) {
            RepoImportDialog(
                onDismiss = { showRepoDialog = false },
                onImport = { repoUrl, scriptPaths ->
                    viewModel.importFromRepo(repoUrl, scriptPaths)
                    showRepoDialog = false
                }
            )
        }
    }
}

// ==================== SCRIPT CARD WITH CONTEXT MENU ====================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScriptCardWithContextMenu(
    script: ScriptManifest,
    binding: TileBinding?,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onPropertiesClick: () -> Unit,
    onQsBindingClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showContextSheet by remember { mutableStateOf(false) }
    
    GamerXCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextSheet = true }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            ThemeManager.accentColor.value.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = ThemeManager.accentColor.value
                    )
                }
                
                if (binding != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Slot ${binding.slotIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = script.identity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = script.identity.description.ifEmpty { "No description" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    
    // Context Menu Modal Bottom Sheet
    if (showContextSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContextSheet = false },
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(ThemeManager.accentColor.value.copy(0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, null, tint = ThemeManager.accentColor.value)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            script.identity.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "v${script.identity.version} • ${script.identity.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                
                // Menu Options
                ContextSheetOption(Icons.Default.Edit, "Edit Script", ThemeManager.accentColor.value) {
                    showContextSheet = false
                    onEditClick()
                }
                ContextSheetOption(Icons.Default.Info, "Properties", Color.White) {
                    showContextSheet = false
                    onPropertiesClick()
                }
                ContextSheetOption(Icons.Default.Widgets, "QS Tile Binding", Color.White) {
                    showContextSheet = false
                    onQsBindingClick()
                }
                
                Spacer(Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                
                ContextSheetOption(Icons.Default.Delete, "Delete Script", Color(0xFFEF5350)) {
                    showContextSheet = false
                    onDeleteClick()
                }
            }
        }
    }
}

@Composable
fun ContextSheetOption(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

// ==================== EMPTY STATE ====================

@Composable
fun EmptyState(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No Scripts Yet",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = "Create or import your first script applet",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCreateClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create")
            }
            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import")
            }
        }
    }
}

// ==================== CREATE WIZARD DIALOG ====================

@Composable
fun CreateScriptWizardDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
    onCreateAndEdit: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { 
            Text("Create Script", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GamerXTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Script Name",
                    placeholder = "My Awesome Script"
                )
                GamerXTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description (Optional)",
                    placeholder = "What does this script do?"
                )
                
                Text(
                    "Tip: After creating, use the editor to add files and set the entry point.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { 
                        if (name.isNotBlank()) {
                            onCreate(name, description)
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Create", color = Color.Gray)
                }
                Button(
                    onClick = { 
                        if (name.isNotBlank()) {
                            onCreateAndEdit(name, description)
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create & Edit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

// ==================== PROPERTIES DIALOG ====================

@Composable
fun ScriptPropertiesDialog(
    script: ScriptManifest,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { 
            Text("Script Properties", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PropertyRow("ID", script.id)
                PropertyRow("Name", script.identity.name)
                PropertyRow("Version", script.identity.version)
                PropertyRow("Author", script.identity.author)
                PropertyRow("Entry Point", script.execution.entryPoint)
                PropertyRow("Engine", script.execution.engine)
                PropertyRow("Timeout", "${script.execution.timeout}s")
                
                if (script.ui.inputs.isNotEmpty()) {
                    PropertyRow("Inputs", "${script.ui.inputs.size} field(s)")
                }
                
                // Capabilities
                Text("Capabilities", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (script.capabilities.rootRequired) CapabilityChip("Root")
                    if (script.capabilities.qsCompatible) CapabilityChip("QS")
                    if (script.capabilities.headlessCapable) CapabilityChip("Headless")
                    if (script.capabilities.networkAccess) CapabilityChip("Network")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ThemeManager.accentColor.value)
            }
        }
    )
}

@Composable
fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CapabilityChip(label: String) {
    Box(
        modifier = Modifier
            .background(ThemeManager.accentColor.value.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ThemeManager.accentColor.value)
    }
}

// ==================== QS BINDING DIALOG ====================

@Composable
fun QsBindingDialog(
    script: ScriptManifest,
    currentBinding: TileBinding?,
    allBindings: TileBindingsConfig,
    onDismiss: () -> Unit,
    onBind: (Int, String, TileMode, List<String>) -> Unit,  // slot, iconName, mode, quickSelectOptions
    onUnbind: (Int) -> Unit
) {
    var slotText by remember { mutableStateOf(currentBinding?.slotIndex?.toString() ?: "") }
    var selectedIcon by remember { mutableStateOf(currentBinding?.iconName ?: "play") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    
    // Derive mode from script manifest
    val scriptMode = when (script.capabilities.tileMode.lowercase()) {
        "toggle" -> TileMode.TOGGLE
        "popup" -> TileMode.POPUP
        "quick_select" -> TileMode.QUICK_SELECT
        else -> TileMode.EXECUTE
    }
    
    val modeLabel = when (scriptMode) {
        TileMode.EXECUTE -> "Execute"
        TileMode.TOGGLE -> "Toggle On/Off"
        TileMode.POPUP -> "Popup"
        TileMode.QUICK_SELECT -> "Quick Select"
    }
    
    val modeDescription = when (scriptMode) {
        TileMode.EXECUTE -> "Single tap runs the script"
        TileMode.TOGGLE -> "Tap to switch On/Off state"
        TileMode.POPUP -> "Shows options before execution"
        TileMode.QUICK_SELECT -> "Cycles through preset values"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TileIcons.getIcon(selectedIcon), null, tint = ThemeManager.accentColor.value)
                Spacer(Modifier.width(12.dp))
                Text("Quick Settings Tile", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current binding status
                if (currentBinding != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Currently bound to Slot ${currentBinding.slotIndex}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                // Slot Number Input
                Text("Slot Number (1-15)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                OutlinedTextField(
                    value = slotText,
                    onValueChange = { 
                        slotText = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    placeholder = { Text("e.g., 1", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.accentColor.value,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ThemeManager.accentColor.value
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Icon Selector
                Text("Tile Icon", style = MaterialTheme.typography.labelMedium, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showIconPicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            TileIcons.getIcon(selectedIcon),
                            null,
                            tint = ThemeManager.accentColor.value,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(selectedIcon.replaceFirstChar { it.uppercase() }, color = Color.White)
                    }
                    Icon(Icons.Default.ExpandMore, null, tint = Color.Gray)
                }
                
                // Mode Info (read-only, from script manifest)
                Text("Tile Mode", style = MaterialTheme.typography.labelMedium, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThemeManager.accentColor.value.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (scriptMode) {
                            TileMode.EXECUTE -> Icons.Default.PlayArrow
                            TileMode.TOGGLE -> Icons.Default.ToggleOn
                            TileMode.POPUP -> Icons.Default.OpenInNew
                            TileMode.QUICK_SELECT -> Icons.Default.Tune
                        },
                        null,
                        tint = ThemeManager.accentColor.value,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(modeLabel, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(modeDescription, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                // Error message
                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Unbind button
                if (currentBinding != null) {
                    TextButton(
                        onClick = { onUnbind(currentBinding.slotIndex) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Binding")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slot = slotText.toIntOrNull()
                    when {
                        slot == null || slot < 1 || slot > 15 -> {
                            errorMessage = "Enter a valid slot number (1-15)"
                        }
                        else -> {
                            val existingBinding = allBindings.getBinding(slot)
                            if (existingBinding != null && existingBinding.scriptId != script.id) {
                                errorMessage = "Slot $slot is used by '${existingBinding.label}'"
                            } else {
                                onBind(slot, selectedIcon, scriptMode, emptyList())
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
            ) {
                Text("Bind")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
    
    // Icon Picker Dialog (instead of ModalBottomSheet)
    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Choose Icon", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 400.dp)
                ) {
                    TileIcons.categories.forEach { category ->
                        Text(
                            category,
                            style = MaterialTheme.typography.labelMedium,
                            color = ThemeManager.accentColor.value,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        val categoryIcons = TileIcons.getIconsByCategory(category)
                        val rows = categoryIcons.chunked(5)
                        
                        rows.forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { tileIcon ->
                                    val isSelected = selectedIcon == tileIcon.name
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                if (isSelected) 2.dp else 0.dp,
                                                if (isSelected) ThemeManager.accentColor.value else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedIcon = tileIcon.name
                                                showIconPicker = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            tileIcon.icon,
                                            contentDescription = tileIcon.name,
                                            tint = if (isSelected) ThemeManager.accentColor.value else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showIconPicker = false }) { Text("Close", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun TileModeOption(
    mode: TileMode,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                if (isSelected) 1.dp else 0.dp,
                if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemeManager.accentColor.value,
                unselectedColor = Color.Gray
            )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}


// ==================== REPO IMPORT DIALOG ====================

@Composable
fun RepoImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, List<String>) -> Unit
) {
    var repoUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var availableScripts by remember { mutableStateOf<List<com.gamerx.manager.feature.script.data.ScriptRepoImporter.RepoScript>>(emptyList()) }
    var selectedScripts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, null, tint = ThemeManager.accentColor.value)
                Spacer(Modifier.width(12.dp))
                Text("Import from Git", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter a GitHub repository URL containing scripts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { 
                        repoUrl = it
                        errorMessage = null
                        availableScripts = emptyList()
                        selectedScripts = emptySet()
                    },
                    placeholder = { Text("https://github.com/user/repo", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Link, null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.accentColor.value,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ThemeManager.accentColor.value
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Fetch Scripts Button
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            val result = com.gamerx.manager.feature.script.data.ScriptRepoImporter.fetchScriptsFromRepo(repoUrl)
                            result.fold(
                                onSuccess = { scripts ->
                                    availableScripts = scripts
                                    if (scripts.isEmpty()) {
                                        errorMessage = "No scripts found in repository"
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = e.message ?: "Failed to fetch scripts"
                                }
                            )
                            isLoading = false
                        }
                    },
                    enabled = repoUrl.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Fetching...")
                    } else {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Find Scripts")
                    }
                }
                
                // Error
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                
                // Available Scripts
                if (availableScripts.isNotEmpty()) {
                    Text("Available Scripts:", color = Color.White, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        availableScripts.forEach { script ->
                            val isSelected = selectedScripts.contains(script.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .clickable {
                                        selectedScripts = if (isSelected) {
                                            selectedScripts - script.path
                                        } else {
                                            selectedScripts + script.path
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedScripts = if (it) {
                                            selectedScripts + script.path
                                        } else {
                                            selectedScripts - script.path
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ThemeManager.accentColor.value
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(script.name, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(script.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(repoUrl, selectedScripts.toList()) },
                enabled = selectedScripts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
            ) {
                Text("Import (${selectedScripts.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

// ==================== SCRIPT CONFIG CONTENT ====================

@Composable
fun ScriptConfigContent(
    script: ScriptManifest,
    viewModel: ScriptViewModel,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    var inputs by remember { mutableStateOf<MutableMap<String, Any>>(mutableMapOf()) }
    var isLoadingState by remember { mutableStateOf(true) }
    
    val executionOutput by viewModel.executionOutput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    LaunchedEffect(script.id) {
        val persisted = viewModel.loadScriptState(script.id)
        val initialMap = mutableMapOf<String, Any>()
        
        script.ui.inputs.forEach { input ->
            when (input) {
                is ScriptInput.TextInput -> initialMap[input.id] = input.default
                is ScriptInput.NumberInput -> initialMap[input.id] = input.default
                is ScriptInput.ToggleInput -> initialMap[input.id] = input.default
                is ScriptInput.SelectInput -> initialMap[input.id] = input.default ?: ""
                is ScriptInput.MultiSelectInput -> initialMap[input.id] = input.default
                is ScriptInput.SliderInput -> initialMap[input.id] = input.default
                is ScriptInput.InfoInput -> {}
                is ScriptInput.FileInput -> initialMap[input.id] = ""
            }
        }
        
        persisted.forEach { (k, v) -> initialMap[k] = v }
        inputs = initialMap
        isLoadingState = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.identity.name, 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "v${script.identity.version} • ${script.identity.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (isLoadingState) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ThemeManager.accentColor.value)
            }
        } else {
            // Render Inputs
            script.ui.inputs.forEach { input ->
                val isVisible = evaluateDependency(input.dependency, inputs)
                
                if (isVisible) {
                    RenderInput(
                        input = input,
                        value = inputs[input.id],
                        onValueChange = { newValue ->
                            inputs = inputs.toMutableMap().apply { this[input.id] = newValue }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Execute Button - For toggle scripts, show On/Off toggle instead
            val isToggleScript = script.capabilities.tileMode.lowercase() == "toggle"
            var toggleState by remember { mutableStateOf(false) }
            
            if (isToggleScript) {
                // Toggle On/Off UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (toggleState) ThemeManager.accentColor.value.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .clickable(enabled = !isExecuting) {
                            toggleState = !toggleState
                            viewModel.executeScript(script, inputs + mapOf("toggle_state" to toggleState.toString()))
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (toggleState) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            null,
                            tint = if (toggleState) ThemeManager.accentColor.value else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (toggleState) "ON" else "OFF",
                                fontWeight = FontWeight.Bold,
                                color = if (toggleState) ThemeManager.accentColor.value else Color.Gray
                            )
                            Text(
                                "Tap to toggle",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ThemeManager.accentColor.value,
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else {
                // Normal Execute Button
                Button(
                    onClick = { viewModel.executeScript(script, inputs) },
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("RUN SCRIPT", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (executionOutput != null) {
                Spacer(Modifier.height(24.dp))
                TerminalConsole(
                    output = executionOutput!!,
                    isExecuting = isExecuting
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ==================== INPUT RENDERER ====================

@Composable
fun RenderInput(
    input: ScriptInput,
    value: Any?,
    onValueChange: (Any) -> Unit
) {
    when (input) {
        is ScriptInput.TextInput -> {
            GamerXTextField(
                value = value as? String ?: "",
                onValueChange = { onValueChange(it) },
                label = input.label,
                placeholder = input.placeholder,
                description = input.description
            )
        }
        
        is ScriptInput.NumberInput -> {
            // Use local text state to properly handle backspace and editing
            var textValue by remember(value) { 
                mutableStateOf(value?.toString()?.replace(".0", "") ?: input.default.toInt().toString()) 
            }
            GamerXTextField(
                value = textValue,
                onValueChange = { newVal ->
                    // Filter to only allow digits
                    val filtered = newVal.filter { it.isDigit() }
                    textValue = filtered
                    // Convert to number and notify parent
                    val num = filtered.toIntOrNull() ?: input.default.toInt()
                    onValueChange(num)
                },
                label = input.label,
                description = input.description,
                isNumber = true
            )
        }
        
        is ScriptInput.ToggleInput -> {
            GamerXSwitch(
                checked = value as? Boolean ?: false,
                onCheckedChange = { onValueChange(it) },
                label = input.label,
                description = input.description
            )
        }
        
        is ScriptInput.SelectInput -> {
            GamerXDropdown(
                selectedValue = value as? String ?: "",
                onValueChange = { onValueChange(it) },
                label = input.label,
                options = input.options.map { it.value to it.label },
                description = input.description
            )
        }
        
        is ScriptInput.MultiSelectInput -> {
            @Suppress("UNCHECKED_CAST")
            val selectedList = (value as? List<String>) ?: emptyList()
            GamerXMultiSelect(
                selectedValues = selectedList,
                onValuesChange = { onValueChange(it) },
                label = input.label,
                options = input.options.map { it.value to it.label },
                description = input.description
            )
        }
        
        is ScriptInput.SliderInput -> {
            GamerXSlider(
                value = (value as? Number)?.toFloat() ?: input.default,
                onValueChange = { onValueChange(it) },
                label = input.label,
                min = input.min,
                max = input.max,
                step = input.step,
                unit = input.unit,
                showValue = input.showValue,
                description = input.description
            )
        }
        
        is ScriptInput.InfoInput -> {
            GamerXInfoCard(
                label = input.label,
                description = input.description,
                style = input.style
            )
        }
        
        is ScriptInput.FileInput -> {
            GamerXTextField(
                value = value as? String ?: "",
                onValueChange = { onValueChange(it) },
                label = input.label,
                placeholder = "File path...",
                description = input.description
            )
        }
    }
}

// ==================== DEPENDENCY EVALUATOR ====================

fun evaluateDependency(dep: InputDependency?, inputs: Map<String, Any>): Boolean {
    if (dep == null) return true
    
    val targetValue = inputs[dep.target]?.toString() ?: ""
    
    return when (dep.condition) {
        "equals" -> targetValue == dep.value
        "not_equals" -> targetValue != dep.value
        "contains" -> targetValue.contains(dep.value, ignoreCase = true)
        "greater_than" -> {
            val target = targetValue.toDoubleOrNull() ?: 0.0
            val expected = dep.value.toDoubleOrNull() ?: 0.0
            target > expected
        }
        "less_than" -> {
            val target = targetValue.toDoubleOrNull() ?: 0.0
            val expected = dep.value.toDoubleOrNull() ?: 0.0
            target < expected
        }
        else -> true
    }
}
