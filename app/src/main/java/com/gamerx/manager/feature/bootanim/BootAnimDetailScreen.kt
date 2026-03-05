package com.gamerx.manager.feature.bootanim

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootAnimDetailScreen(navController: NavController, animName: String) {
    val viewModel: BootAnimViewModel = viewModel() 
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val anim = BootAnimViewModel.anims.find { it.name == animName }
    
    // UI State for Install
    val statusText by viewModel.statusText // Observer
    val installedAnim by BootAnimViewModel.installedAnim
    val isInstalled = installedAnim.contains(anim?.path ?: "xxx")
    val isDefault = animName.lowercase() == "stock" || animName.lowercase() == "default" // Simple check
    
    var isInstalling by remember { mutableStateOf(false) }

    // React to status text to stop spinner
    LaunchedEffect(statusText) {
        if (statusText.contains("Applied") || statusText.contains("Error")) {
            isInstalling = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(animName, color = Color.White) }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Hide Delete for Defaults (heuristic check)
                    if (anim != null && !isDefault && !anim.path.contains("/system/media")) {
                         IconButton(onClick = { 
                            viewModel.deleteBootAnim(anim, context.cacheDir)
                            navController.popBackStack() 
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
             Box(
                 modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha=0.9f))
                    .windowInsetsPadding(WindowInsets.navigationBars) 
                    .padding(16.dp)
                    .padding(bottom = 80.dp) 
             ) {
                 if (isInstalling) {
                     // Installation Progress (Indeterminate now, as VM doesn't give % yet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = ThemeManager.accentColor.value, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Applying... Please wait",
                            color = ThemeManager.accentColor.value
                        )
                    }
                } else if (isInstalled || statusText.contains("Applied")) {
                    // Success / Reboot State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Applied Successfully", color = Color.Green, style = MaterialTheme.typography.titleMedium)
                            Text("Reboot required.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Button(
                            onClick = { viewModel.rebootSystem() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Reboot", color = Color.White)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (anim != null) {
                                isInstalling = true
                                viewModel.installBootAnim(anim.path, context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Apply Bootanimation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
             }
        },
        containerColor = Color.Black.copy(alpha=0.9f)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Pro Preview Player (Fixed Ratio/Size)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.7f) // Reduce width to 70%
                    .aspectRatio(9f/16f) // Force Phone Ratio Container
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.DarkGray)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (anim != null && anim.previewPath.isNotEmpty()) {
                         val previewFile = File(anim.previewPath)
                         if (previewFile.isDirectory) {
                             // Pass aspect ratio info
                             val isStandardRatio = (anim.height.toFloat() / anim.width.toFloat()) > 1.7f // e.g. 16:9 or 20:9
                             BootAnimFramePlayer(anim.previewPath, isStandardRatio)
                         } else {
                             Text("No frames found", color = Color.Gray)
                         }
                    } else {
                        Text("No Preview", color = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = animName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
             if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusText, color = if (statusText.contains("Error")) Color.Red else Color.Green)
            }
        }
    }
}

@Composable
fun BootAnimFramePlayer(folderPath: String, isStandardRatio: Boolean) {
    var currentFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(folderPath) {
        withContext(Dispatchers.IO) {
            val dir = File(folderPath)
            if (dir.exists() && dir.isDirectory) {
                // Get PNGs recursively
                val frames = dir.walk()
                    .filter { f -> f.isFile && f.name.endsWith(".png") && f.name != "thumbnail.png" }
                    .sortedBy { it.name } // Prioritize numeric sorting if possible, but name is okay for part0
                    .toList()
                    
                if (frames.isNotEmpty()) {
                    var index = 0
                    while (true) {
                        val file = frames[index]
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        withContext(Dispatchers.Main) {
                            currentFrame = bmp
                        }
                        delay(40) // 25 FPS for cinematic feel
                        index = (index + 1) % frames.size
                    }
                }
            }
        }
    }
    
    if (currentFrame != null) {
        androidx.compose.foundation.Image(
            bitmap = currentFrame!!.asImageBitmap(),
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            // If aspect ratio matches phone (approx), CROP/FILL. If weird (1080x380), FIT.
            contentScale = if (isStandardRatio) androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             CircularProgressIndicator(color = ThemeManager.accentColor.value)
         }
    }
}
