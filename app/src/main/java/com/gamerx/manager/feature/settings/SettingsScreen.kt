package com.gamerx.manager.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gamerx.manager.R
import com.gamerx.manager.ui.theme.ThemeManager

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Dev Info Section
        Text(
            text = "Developer Info",
            style = MaterialTheme.typography.titleMedium,
            color = ThemeManager.accentColor.value,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeManager.getCardColor()),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Developed By",
                    value = "GamerX Team"
                )
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    value = "v2.0.0 (Reborn)"
                )
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Build Type",
                    value = "Alpha / Debug"
                )
            }
        }
        
        // About App Section
        Text(
            text = "About GamerX Manager",
            style = MaterialTheme.typography.titleMedium,
            color = ThemeManager.accentColor.value,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeManager.getCardColor()),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "The ultimate tool for Android customization and performance tuning. Crafted with Jetpack Compose.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemeManager.accentColor.value,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
