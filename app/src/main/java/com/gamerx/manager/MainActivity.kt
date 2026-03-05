package com.gamerx.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gamerx.manager.ui.navigation.MainLayout
import com.gamerx.manager.ui.theme.GamerXTheme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        com.gamerx.manager.ui.theme.ThemeManager.init(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Root Check
        var isRooted = false
        try {
            val process = Runtime.getRuntime().exec("su -c id")
            isRooted = process.waitFor() == 0
        } catch (e: Exception) {
            isRooted = false
        }

        setContent {
            GamerXTheme {
                var showRootDialog by remember { mutableStateOf(!isRooted) }

                if (showRootDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Prevent dismiss */ },
                        title = { Text("Root Not Detected") },
                        text = { Text("GamerX Manager requires root access to function properly.") },
                        confirmButton = {
                            TextButton(onClick = { showRootDialog = false }) {
                                Text("Okay")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) {
                                Text("Close App")
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainLayout(navController = navController)
                }
            }
        }
    }
}
