package com.gamerx.manager.feature.deviceinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    navController: NavController,
    viewModel: DeviceInfoViewModel = viewModel()
) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Device Information", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val grouped = viewModel.deviceInfoList.value.groupBy { it.category }
                
                grouped.forEach { (category, specs) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            color = com.gamerx.manager.ui.theme.GamerXRed,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GamerXCard(padding = 0.dp) {
                            Column {
                                specs.forEachIndexed { index, spec ->
                                    InfoItem(spec)
                                    if (index < specs.size - 1) {
                                        Divider(color = Color.White.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun InfoItem(spec: DeviceSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(spec.label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(spec.value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
