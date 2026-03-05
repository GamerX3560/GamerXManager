package com.gamerx.manager.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.theme.GamerXRed
import com.gamerx.manager.ui.theme.SuccessColor

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    // Background is handled by MainLayout now (ThemeManager)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Decorative Top Gradient Circle - Only for Colorful Theme
        if (com.gamerx.manager.ui.theme.ThemeManager.currentTheme.value == com.gamerx.manager.ui.theme.AppTheme.COLORFUL) {
            Box(
                modifier = Modifier
                    .offset(y = (-100).dp, x = 100.dp)
                    .size(300.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(com.gamerx.manager.ui.theme.ThemeManager.accentColor.value.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            
            // Hero Section
            Column {
                Text(
                    text = "GamerX",
                    style = MaterialTheme.typography.displayMedium,
                    color = com.gamerx.manager.ui.theme.GamerXRed,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manager",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.offset(y = (-8).dp)
                )
            }

            // Status Row (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(
                    label = if (viewModel.rootStatus.value) "ROOTED" else "NO ROOT",
                    isActive = viewModel.rootStatus.value,
                    icon = if (viewModel.rootStatus.value) Icons.Default.CheckCircle else Icons.Default.Warning
                )
                StatusChip(
                    label = viewModel.selinuxStatus.value.uppercase(),
                    isActive = viewModel.selinuxStatus.value.contains("Enforcing", ignoreCase = true),
                    activeColor = Color(0xFF00E5FF),
                    icon = Icons.Default.Security
                )
            }
            
            // Device Info Compact
            GamerXCard(
                backgroundColor = Color(0xFF1E1E1E).copy(alpha = 0.5f),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Device", fontSize = 12.sp, color = Color.Gray)
                        Text(viewModel.deviceModel.value, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Android", fontSize = 12.sp, color = Color.Gray)
                        Text(viewModel.androidVersion.value.substringBefore(" "), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Features Grid Title
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            // Features Grid
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard(
                        title = "Performance",
                        icon = Icons.Default.Speed,
                        description = "CPU / GPU",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(Color(0xFFD50000), Color(0xFF8E0000)),
                        onClick = { navController.navigate(com.gamerx.manager.ui.navigation.Screen.Performance.route) }
                    )
                    FeatureCard(
                        title = "Tweaks",
                        icon = Icons.Default.SettingsSuggest,
                        description = "System Props",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(Color(0xFF2962FF), Color(0xFF002171)),
                        onClick = { navController.navigate(com.gamerx.manager.ui.navigation.Screen.Tweaks.route) }
                    )
                }
                
                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard(
                        title = "Spoofer",
                        icon = Icons.Default.Smartphone,
                        description = "Identity",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(Color(0xFF00C853), Color(0xFF005005)),
                        onClick = { navController.navigate(com.gamerx.manager.ui.navigation.Screen.Spoofer.route) }
                    )
                    FeatureCard(
                        title = "Linux",
                        icon = Icons.Default.Terminal,
                        description = "Environment",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(Color(0xFFFFAB00), Color(0xFFFF6D00)),
                        onClick = { navController.navigate(com.gamerx.manager.ui.navigation.Screen.Linux.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    isActive: Boolean,
    activeColor: Color = SuccessColor,
    icon: ImageVector
) {
    Surface(
        color = if (isActive) activeColor.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isActive) activeColor.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isActive) activeColor else Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isActive) activeColor else Color.Red,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(
    title: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(com.gamerx.manager.ui.theme.SurfaceVariant, com.gamerx.manager.ui.theme.SurfaceVariant),
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.linearGradient(gradient))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}



