package com.gamerx.manager.feature.tools

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.navigation.Screen
import com.gamerx.manager.ui.theme.GamerXRed
import com.gamerx.manager.ui.theme.ThemeManager

@Composable
fun ToolsScreen(navController: NavController) {
    var showShellDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showShellDialog) {
        ShellSelectionDialog(
            onDismiss = { showShellDialog = false },
            onLaunch = { isRoot ->
                launchTerminal(context, isRoot)
                showShellDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Tools Hub",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Tools Grid
        val tools = listOf(
            ToolItem("Icon Packs", Icons.Default.Style, Screen.IconPack.route),
            ToolItem("Boot Anim", Icons.Default.Animation, Screen.BootAnim.route),
            ToolItem("Android Shell", Icons.Default.Terminal, "shell_dialog"), // Custom route key
            ToolItem("Device Spoofer", Icons.Default.Smartphone, Screen.Spoofer.route),
            ToolItem("GamerX Linux", Icons.Default.Computer, Screen.Linux.route),
            ToolItem("System Sounds", Icons.Default.Speaker, Screen.SystemSounds.route),
            ToolItem("Scripts", Icons.Default.Code, Screen.Scripts.route),
            ToolItem("Device Info", Icons.Default.Info, Screen.DeviceInfo.route)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tools) { tool ->
                ToolCard(tool = tool, onClick = {
                    if (tool.route == "shell_dialog") {
                        showShellDialog = true
                    } else {
                        navController.navigate(tool.route)
                    }
                })
            }
        }
    }
}

@Composable
fun ShellSelectionDialog(onDismiss: () -> Unit, onLaunch: (Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GamerXCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Shell", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { onLaunch(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Android Shell", color = Color.Cyan)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onLaunch(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = GamerXRed.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Android Root Shell", color = GamerXRed)
                }
            }
        }
    }
}

private fun launchTerminal(context: Context, isRoot: Boolean) {
    val packageNames = listOf(
        "com.gamerx.terminal",
        "com.offsec.nethunter",
        "com.termux"
    )

    var intent: Intent? = null
    for (pkg in packageNames) {
        intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) break
    }

    if (intent != null) {
        if (isRoot) intent.putExtra("isRoot", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Terminal App not found!", Toast.LENGTH_SHORT).show()
    }
}

data class ToolItem(val name: String, val icon: ImageVector, val route: String)

@Composable
fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeManager.getCardColor()),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = ThemeManager.accentColor.value,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
