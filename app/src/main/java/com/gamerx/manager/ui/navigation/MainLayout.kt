package com.gamerx.manager.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gamerx.manager.feature.customize.CustomizeScreen
import com.gamerx.manager.feature.home.HomeScreen
import com.gamerx.manager.feature.settings.SettingsScreen
import com.gamerx.manager.feature.tools.ToolsScreen
import com.gamerx.manager.feature.performance.PerformanceScreen
import com.gamerx.manager.feature.tweaks.TweaksScreen
import com.gamerx.manager.feature.spoofer.SpooferScreen
import com.gamerx.manager.feature.linux.LinuxScreen
import com.gamerx.manager.feature.deviceinfo.DeviceInfoScreen
import com.gamerx.manager.feature.iconpack.IconPackScreen
import com.gamerx.manager.feature.bootanim.BootAnimationScreen
import com.gamerx.manager.feature.shell.ShellScreen
import com.gamerx.manager.feature.script.ui.ScriptScreen
import com.gamerx.manager.feature.script.ui.ScriptEditorScreen

@Composable
fun MainLayout(navController: NavHostController) {
    val containerColor = Color.Transparent 
    // We use a Box to layer the background, then a Scaffold-like structure for content + nav
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.gamerx.manager.ui.theme.ThemeManager.getBackgroundBrush())
    ) {
        // Custom Background Image Layer
        // Custom Background Image Layer
        val customBg = com.gamerx.manager.ui.theme.ThemeManager.customBackgroundUri.value
        val blurRadius = (com.gamerx.manager.ui.theme.ThemeManager.blurIntensity.value * 0.2f).dp
        
        if (customBg != null) {
            if (customBg is Int) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(customBg),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(blurRadius)
                )
            } else if (customBg is String) {
                // Load from URI using Coil
                coil.compose.AsyncImage(
                    model = customBg,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(blurRadius)
                )
            }
        }
        
        // Wallpaper Dim Layer (Black Overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = com.gamerx.manager.ui.theme.ThemeManager.wallpaperDim.value))
        )

        // Special Effects Overlay (Snow, Rain, Leaves)
        val specialEffect = com.gamerx.manager.ui.theme.ThemeManager.specialEffect.value
        com.gamerx.manager.ui.components.SpecialEffectOverlay(effect = specialEffect)


        // Content
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (com.gamerx.manager.ui.theme.ThemeManager.navStyle.value == com.gamerx.manager.ui.theme.NavStyle.CLASSIC) 80.dp else 0.dp) 
        ) {
            // Main Tabs
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Tools.route) { ToolsScreen(navController) }
            composable(Screen.Customize.route) { CustomizeScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            
            // Sub Features
            composable(Screen.Performance.route) { PerformanceScreen(navController) }
            composable(Screen.Tweaks.route) { TweaksScreen(navController) }
            composable(Screen.Spoofer.route) { SpooferScreen(navController) }
            composable(Screen.Linux.route) { LinuxScreen(navController) }
            composable(Screen.DeviceInfo.route) { DeviceInfoScreen(navController) }
            composable(Screen.IconPack.route) { IconPackScreen(navController) }
            composable(Screen.BootAnim.route) { BootAnimationScreen(navController) }
            composable(Screen.Shell.route) { ShellScreen(navController) }
            composable(Screen.Scripts.route) { 
                ScriptScreen(
                    onNavigateToEditor = { scriptId ->
                        navController.navigate("script_editor/$scriptId")
                    }
                )
            }
            
            // Script Editor Screen
            composable(
                route = Screen.ScriptEditor.route,
                arguments = listOf(androidx.navigation.navArgument("scriptId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val scriptId = backStackEntry.arguments?.getString("scriptId") ?: return@composable
                ScriptEditorScreen(
                    scriptId = scriptId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Detail Screens
            composable(
                route = Screen.IconPackDetail.route,
                arguments = listOf(androidx.navigation.navArgument("packName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val packName = backStackEntry.arguments?.getString("packName") ?: return@composable
                com.gamerx.manager.feature.iconpack.IconPackDetailScreen(navController = navController, packName = packName)
            }
            composable(
                route = Screen.BootAnimDetail.route,
                arguments = listOf(androidx.navigation.navArgument("animName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val animName = backStackEntry.arguments?.getString("animName") ?: return@composable
                com.gamerx.manager.feature.bootanim.BootAnimDetailScreen(navController = navController, animName = animName)
            }
        }
        
        // Navigation Bar
        val navStyle = com.gamerx.manager.ui.theme.ThemeManager.navStyle.value
        val modifier = when (navStyle) {
            com.gamerx.manager.ui.theme.NavStyle.FLOATING -> Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().height(70.dp)
            com.gamerx.manager.ui.theme.NavStyle.PILL -> Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).width(280.dp).height(64.dp)
            com.gamerx.manager.ui.theme.NavStyle.CLASSIC -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(80.dp)
        }
        
        GamerXBottomBar(
             navController = navController, 
             modifier = modifier,
             style = navStyle
        )
    }
}

@Composable
fun GamerXBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    style: com.gamerx.manager.ui.theme.NavStyle
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val items = listOf(
        Screen.Home to Icons.Default.Dashboard,
        Screen.Tools to Icons.Default.Build,
        Screen.Customize to Icons.Default.Palette,
        Screen.Settings to Icons.Default.Settings
    )

    // Shapes and Colors
    val shape = when (style) {
        com.gamerx.manager.ui.theme.NavStyle.FLOATING -> androidx.compose.foundation.shape.RoundedCornerShape(35.dp)
        com.gamerx.manager.ui.theme.NavStyle.PILL -> androidx.compose.foundation.shape.RoundedCornerShape(50)
        com.gamerx.manager.ui.theme.NavStyle.CLASSIC -> androidx.compose.ui.graphics.RectangleShape
    }
    
    // Fix: Glassmorphism Nav Bar should be Black if Liquid Glass theme, or Dark if others
    // User requested "black nevigation bar not white" for Liquid Glass.
    // Assuming getCardColor handles this, but let's be explicit for Nav Bar to ensure contrast
    val theme = com.gamerx.manager.ui.theme.ThemeManager.currentTheme.value
    val containerColor = if (style == com.gamerx.manager.ui.theme.NavStyle.CLASSIC || theme == com.gamerx.manager.ui.theme.AppTheme.OLED) {
        Color.Black
    } else {
        // Use a dark translucent color always for nav bar to ensure visibility
        Color(0xFF121212).copy(alpha = 0.9f)
    }

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (style == com.gamerx.manager.ui.theme.NavStyle.PILL) 24.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon) ->
                // Smart Highlighting Logic
                val selected = when (screen) {
                    Screen.Tools -> {
                        // Highlight Tools if we are in Tools OR any feature/detail screen
                        val route = currentDestination?.route
                        route == Screen.Tools.route || 
                        route == Screen.Performance.route ||
                        route == Screen.Tweaks.route ||
                        route == Screen.Spoofer.route ||
                        route == Screen.Linux.route ||
                        route == Screen.DeviceInfo.route ||
                        route == Screen.IconPack.route ||
                        route == Screen.BootAnim.route ||
                        route == Screen.Shell.route ||
                        route == Screen.Scripts.route ||
                        route?.startsWith("icon_pack_detail") == true ||
                        route?.startsWith("boot_anim_detail") == true ||
                        route?.startsWith("script_editor") == true
                    }
                    else -> currentDestination?.hierarchy?.any { it.route == screen.route } == true
                }
                
                val color = if (selected) com.gamerx.manager.ui.theme.ThemeManager.accentColor.value else Color.Gray
                
                IconButton(
                    onClick = {
                        val isAtRoute = currentDestination?.route == screen.route
                        if (selected && isAtRoute) {
                            // Already at root, do nothing
                        } else {
                             navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon, 
                            contentDescription = null, 
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        if (selected && style != com.gamerx.manager.ui.theme.NavStyle.PILL) {
                             // Pill style is too small for indicators typically
                             Spacer(Modifier.height(4.dp))
                             Box(
                                Modifier.size(4.dp).background(color, androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Extension to capitalize first letter (simple version)
private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
