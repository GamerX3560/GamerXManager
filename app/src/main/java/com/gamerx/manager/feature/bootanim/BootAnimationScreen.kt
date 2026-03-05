package com.gamerx.manager.feature.bootanim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.theme.ThemeManager
import com.gamerx.manager.ui.components.GamerXCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootAnimationScreen(navController: NavController) {
    val viewModel: BootAnimViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showImportDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importName by remember { mutableStateOf("") }
    
    // File Picker for Import
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            importUri = uri
            showImportDialog = true // Trigger Dialog
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Bootanimation", color = Color.White) },
            text = {
                Column {
                    Text("Enter a unique name for this animation:", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ThemeManager.accentColor.value,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importName.isNotEmpty() && importUri != null) {
                            viewModel.importBootAnim(importUri!!, context, importName)
                            showImportDialog = false
                            importName = "" // Reset
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.DarkGray,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LaunchedEffect(Unit) {
        if (BootAnimViewModel.anims.isEmpty()) {
            viewModel.loadAnims(context.cacheDir)
        }
    }
    
    LaunchedEffect(viewModel.statusText.value) {
        if (viewModel.statusText.value.isNotEmpty()) {
            android.widget.Toast.makeText(context, viewModel.statusText.value, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.statusText.value = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Boot Animations",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                actions = {
                    IconButton(onClick = { launcher.launch("application/zip") }) {
                        Icon(Icons.Default.Add, contentDescription = "Import", tint = ThemeManager.accentColor.value)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            
            if (viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ThemeManager.accentColor.value)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(BootAnimViewModel.anims) { anim ->
                        GamerXCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) 
                                .clickable { 
                                    try {
                                        val route = com.gamerx.manager.ui.navigation.Screen.BootAnimDetail.route.replace("{animName}", android.net.Uri.encode(anim.name))
                                        navController.navigate(route)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Nav Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Logic for Thumbnail Preview
                                    val previewFile = File(anim.previewPath)
                                    if (previewFile.isDirectory) {
                                         // Check for thumbnail.png first
                                         val thumb = File(previewFile, "thumbnail.png")
                                         if (thumb.exists()) {
                                             BootAnimImagePreview(thumb.absolutePath)
                                         } else {
                                             // Recursively find first png
                                             val firstFrame = previewFile.walk().filter { f -> f.isFile && f.name.endsWith(".png") }.sortedBy { it.name }.firstOrNull()
                                             if (firstFrame != null) {
                                                 BootAnimImagePreview(firstFrame.absolutePath)
                                             } else {
                                                 Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                             }
                                         }
                                    } else if (anim.previewPath.endsWith(".png") || anim.previewPath.endsWith(".jpg")) {
                                         BootAnimImagePreview(anim.previewPath)
                                    } else if (anim.previewPath.isNotEmpty()) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(48.dp))
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    }
                                }
                                
                                // Title and Applied Status (Simulated)
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = anim.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    // Check applied status from VM
                                    if (BootAnimViewModel.installedAnim.value.contains(anim.name)) {
                                         Text("Applied", color = Color.Green, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BootAnimImagePreview(path: String) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(path) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
             modifier = Modifier.fillMaxSize().background(Color.DarkGray),
             contentAlignment = Alignment.Center
        ) {
             Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
        }
    }
}
