package com.gamerx.manager.feature.shell

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.theme.GamerXRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android Shell", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Shell Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            ShellOptionCard(
                title = "Android Shell",
                description = "Launch Standard Terminal",
                icon = Icons.Default.Terminal,
                onClick = { launchTerminal(context, isRoot = false) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ShellOptionCard(
                title = "Android Root Shell",
                description = "Launch Root Terminal",
                icon = Icons.Default.Terminal,
                isRoot = true,
                onClick = { launchTerminal(context, isRoot = true) }
            )
        }
    }
}

@Composable
fun ShellOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isRoot: Boolean = false,
    onClick: () -> Unit
) {
    GamerXCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isRoot) GamerXRed else Color.Cyan,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun launchTerminal(context: Context, isRoot: Boolean) {
    // Attempt standard package names for the terminal
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
        if (isRoot) {
            // Common extra for requesting root in some terminals, 
            // otherwise user just types 'su'
            intent.putExtra("isRoot", true) 
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "GamerX Terminal not found!", Toast.LENGTH_SHORT).show()
    }
}

