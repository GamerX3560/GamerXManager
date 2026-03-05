package com.gamerx.manager.feature.iconpack

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
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
fun IconPackScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: IconPackViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        if (IconPackViewModel.iconPacks.isEmpty()) {
            viewModel.loadIconPacks(context.cacheDir)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Icon Packs",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

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
                items(IconPackViewModel.iconPacks) { pack ->
                    GamerXCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) 
                            .clickable { 
                                 try {
                                     val route = com.gamerx.manager.ui.navigation.Screen.IconPackDetail.route.replace("{packName}", android.net.Uri.encode(pack.name))
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
                                    .background(Color.DarkGray)
                            ) {
                                if (pack.previewPath.isNotEmpty()) {
                                    AsyncLocalImage(path = pack.previewPath)
                                } else if (pack.iconResId != 0) {
                                    Image(
                                        painter = androidx.compose.ui.res.painterResource(id = pack.iconResId),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    }
                                }
                            }
                            
                            // Title
                            Text(
                                text = pack.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsyncLocalImage(path: String) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(path) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(file.absolutePath)
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = Icons.Default.Palette,
                 contentDescription = null, 
                 tint = Color.Gray, 
                 modifier = Modifier.size(48.dp)
             )
        }
    }
}
