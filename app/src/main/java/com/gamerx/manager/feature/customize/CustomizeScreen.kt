package com.gamerx.manager.feature.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.theme.AppTheme
import com.gamerx.manager.ui.theme.NavStyle
import com.gamerx.manager.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customization", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {


        // 1. Accent Color
        item {
            SectionTitle("Accent Color", Icons.Default.Palette)
            GamerXCard(padding = 16.dp) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 200.dp) // Constrain height inside scrollable
                ) {
                    items(com.gamerx.manager.ui.theme.Palettes) { (color, name) ->
                        ColorOption(color = color, selected = ThemeManager.accentColor.value == color) {
                            ThemeManager.saveAccentColor(color)
                        }
                    }
                }
            }
        }

        // 2. Global Theme
        item {
            SectionTitle("App Theme", Icons.Default.ViewCarousel)
            GamerXCard(padding = 0.dp) {
                Column {
                    ThemeOption(
                        title = "Colorful Cyber (Default)",
                        subtitle = "Immersive gradients and vibrant glow",
                        selected = ThemeManager.currentTheme.value == AppTheme.COLORFUL,
                        onClick = { ThemeManager.saveTheme(AppTheme.COLORFUL) }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "OLED Minimalist",
                        subtitle = "Pure black, high contrast, battery saver",
                        selected = ThemeManager.currentTheme.value == AppTheme.OLED,
                        onClick = { ThemeManager.saveTheme(AppTheme.OLED) }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "Liquid Glass",
                        subtitle = "Blur effects and translucent panels",
                        selected = ThemeManager.currentTheme.value == AppTheme.GLASS,
                        onClick = { ThemeManager.saveTheme(AppTheme.GLASS) }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "Retro Wave",
                        subtitle = "Synthwave visuals, purple/cyan aesthetic",
                        selected = ThemeManager.currentTheme.value == AppTheme.RETRO_WAVE,
                        onClick = { ThemeManager.saveTheme(AppTheme.RETRO_WAVE) }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "Cyber-Contrast",
                        subtitle = "Neon lines on deep black, minimal fill",
                        selected = ThemeManager.currentTheme.value == AppTheme.HIGH_CONTRAST,
                        onClick = { ThemeManager.saveTheme(AppTheme.HIGH_CONTRAST) }
                    )
                }
            }
        }

        // 3. Navigation Style
        item {
            SectionTitle("Navigation Bar", Icons.Default.ViewCarousel)
             GamerXCard(padding = 0.dp) {
                Column {
                    ThemeOption(
                        title = "Floating Island",
                        subtitle = "Modern detached bar",
                        selected = ThemeManager.navStyle.value == NavStyle.FLOATING,
                        onClick = { ThemeManager.saveNavStyle(NavStyle.FLOATING) }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "Classic Dock",
                        subtitle = "Full width standard bar",
                        selected = ThemeManager.navStyle.value == NavStyle.CLASSIC,
                        onClick = { ThemeManager.saveNavStyle(NavStyle.CLASSIC) }
                    )
                     Divider(color = Color.White.copy(alpha = 0.1f))
                    ThemeOption(
                        title = "Minimal Pill",
                        subtitle = "Compact centered capsule",
                        selected = ThemeManager.navStyle.value == NavStyle.PILL,
                        onClick = { ThemeManager.saveNavStyle(NavStyle.PILL) }
                    )
                }
            }
        }
        
        // 4. Background
        item {
             SectionTitle("Background", Icons.Default.Wallpaper)
             GamerXCard(padding = 16.dp) {
                 Column {
                     Text("Presets", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                     Spacer(Modifier.height(8.dp))
                     
                     // Horizontal scroll for presets
                     androidx.compose.foundation.lazy.LazyRow(
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         // Default (None)
                         item {
                             BackgroundOption(
                                 model = null, 
                                 selected = ThemeManager.customBackgroundUri.value == null,
                                 onClick = { ThemeManager.saveBackground(null) }
                             )
                         }
                         
                         items(ThemeManager.Presets) { resId ->
                             BackgroundOption(
                                 model = resId,
                                 selected = ThemeManager.customBackgroundUri.value == resId,
                                 onClick = { ThemeManager.saveBackground(resId) }
                             )
                         }
                     }
                     
                     Spacer(Modifier.height(16.dp))
                     
                     val context = androidx.compose.ui.platform.LocalContext.current
                     val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                     ) { uri: android.net.Uri? ->
                        if (uri != null) {
                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            ThemeManager.saveBackground(uri.toString())
                        }
                     }

                     Button(
                         onClick = { launcher.launch("image/*") },
                         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text("Select from Gallery")
                     }
                 }
             }
        }
        
        // 5. Advanced Effects
        item {
            SectionTitle("Advanced Effects", Icons.Default.Palette)
            GamerXCard(padding = 16.dp) {
                Column {
                    // UI Opacity
                    Text("UI Layer Opacity: ${(ThemeManager.uiLayerOpacity.value * 100).toInt()}%", color = Color.White)
                    Slider(
                        value = ThemeManager.uiLayerOpacity.value,
                        onValueChange = { ThemeManager.saveUiOpacity(it) },
                        colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Wallpaper Dim
                    Text("Wallpaper Dim: ${(ThemeManager.wallpaperDim.value * 100).toInt()}%", color = Color.White)
                    Slider(
                        value = ThemeManager.wallpaperDim.value,
                        onValueChange = { ThemeManager.saveWallpaperDim(it) },
                        colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                    )

                    Spacer(Modifier.height(16.dp))
                    
                    // Blur
                    Text("Blur Intensity: ${(ThemeManager.blurIntensity.value).toInt()}", color = Color.White)
                    Slider(
                        value = ThemeManager.blurIntensity.value,
                        onValueChange = { ThemeManager.saveBlur(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Special Effects Selector
                    Text("Special Effects", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(com.gamerx.manager.ui.theme.SpecialEffect.values()) { effect ->
                             val selected = ThemeManager.specialEffect.value == effect
                             Box(
                                 modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ThemeManager.accentColor.value else Color.White.copy(alpha = 0.1f))
                                    .clickable { ThemeManager.saveSpecialEffect(effect) },
                                 contentAlignment = Alignment.Center
                             ) {
                                 Text(
                                     text = effect.name, 
                                     color = if (selected) Color.White else Color.Gray,
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = FontWeight.Bold
                                 )
                             }
                        }
                    }
                    
                    // Effect Adjustments (Only if effect is selected)
                    if (ThemeManager.specialEffect.value != com.gamerx.manager.ui.theme.SpecialEffect.NONE) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(16.dp))
                        
                        Text("Effect Size: ${String.format("%.1fx", ThemeManager.effectSize.value)}", color = Color.Gray)
                         Slider(
                             value = ThemeManager.effectSize.value,
                             onValueChange = { ThemeManager.saveEffectSize(it) },
                             valueRange = 0.5f..2.0f,
                             colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                        )
                        
                        Text("Effect Speed: ${String.format("%.1fx", ThemeManager.effectSpeed.value)}", color = Color.Gray)
                        Slider(
                             value = ThemeManager.effectSpeed.value,
                             onValueChange = { ThemeManager.saveEffectSpeed(it) },
                             valueRange = 0.1f..3.0f,
                             colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                        )
                        
                        Text("Effect Density (Proportion): ${String.format("%.1fx", ThemeManager.effectDensity.value)}", color = Color.Gray)
                         Slider(
                             value = ThemeManager.effectDensity.value,
                             onValueChange = { ThemeManager.saveEffectDensity(it) },
                             valueRange = 0.5f..2.0f,
                             colors = SliderDefaults.colors(thumbColor = ThemeManager.accentColor.value, activeTrackColor = ThemeManager.accentColor.value)
                        )
                    }
                }
            }
        }
        
        // Spacer for Navigation Bar Clearance
        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
    }
}

@Composable
fun BackgroundOption(model: Any?, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Box(
        modifier = Modifier
            .size(80.dp, 120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        if (model == null) {
             Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                 Text("Default", color = Color.White, style = MaterialTheme.typography.labelSmall)
             }
        } else {
            // Using placeholder coil implementation logic (assuming coil is in project or simple painter)
            // Since I don't see coil imports, using simple AndroidResource or empty for now if logic is complex.
            // But I should use Image if possible.
            // Assumption: R.drawable.bg_preset_X is an Int.
            if (model is Int) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(model),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                 Icon(Icons.Default.Check, null, tint = Color.White)
            }
        }
    }
}


@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.Gray, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ThemeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        if (selected) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ColorOption(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}


