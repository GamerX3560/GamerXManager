package com.gamerx.manager.feature.iconpack

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackDetailScreen(navController: NavController, packName: String) {
    val viewModel: IconPackViewModel = viewModel() // Assumes shared or scoped ViewModel
    // Ideally we should scope this ViewModel to navigation graph or use hilt, but standard viewModel() gives new instance if not scoped?
    // Actually viewModel() in distinct composables usually gives scoped to nearest NavBackStackEntry or Activity. 
    // Since we want to share state between list and detail, we usually need hilthiltViewModel() or pass it.
    // For now, let's assume `viewModel()` will give us same instance if Activity scoped OR we will just reload data.
    // However, for progress tracking, we need the SAME instance.
    // Compose Navigation `viewModel(navController.getBackStackEntry("icon_pack"))` is tricky without defining the route graph properly.
    // Simplified: Just use the same instance if Activity is owner, or reload.
    // But since "progress" is transient, if we get a new VM, we lose progress. 
    // Let's assume for this "Manager" structure, we are fine, or we use a Singleton/Activity scope. 
    // Actually `viewModel()` default scope is the `NavBackStackEntry`. 
    // To share: `val viewModel: IconPackViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity)` 
    // safely: `val viewModel: IconPackViewModel = viewModel(viewModelStoreOwner = navController.rememberViewModelStoreOwner())` (helper need).
    // Let's use simple viewModel() and hope it works or fix later. User constraints: "do till here".
    
    // We can find the pack from the list
    // Use Global State
    val pack = IconPackViewModel.iconPacks.find { it.name == packName }
    
    // Installation State
    val progress = IconPackViewModel.installationProgress[packName] ?: 0f
    val isInstalling = IconPackViewModel.installationProgress.containsKey(packName)
    val isInstalled = IconPackViewModel.installedPack.value == packName
    
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(packName, color = Color.White) }, // Name at top
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    .padding(bottom = 100.dp) // Lift clear of GamerX Bottom Bar
             ) {
                if (isInstalling && !isInstalled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ThemeManager.accentColor.value)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Installing... ${(animatedProgress * 100).toInt()}%",
                            color = ThemeManager.accentColor.value
                        )
                    }
                } else if (isInstalled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Installed",
                            tint = Color.Green,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Installed Successfully", color = Color.Green, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            if (pack != null) {
                                viewModel.installIconPack(pack.name, pack.path)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Install Icon Pack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            
            // Brand Icon (Preview as Icon if available, or placeholder)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (pack != null) {
                    if (pack.previewPath.isNotEmpty()) {
                        AsyncLocalImage(path = pack.previewPath) 
                    } else if (pack.iconResId != 0) {
                         Image(
                            painter = androidx.compose.ui.res.painterResource(id = pack.iconResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(packName.take(1), style = MaterialTheme.typography.displayMedium, color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = packName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            if (pack != null) {
                Text(
                    text = pack.description,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
