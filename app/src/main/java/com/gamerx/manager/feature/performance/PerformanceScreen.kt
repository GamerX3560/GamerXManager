package com.gamerx.manager.feature.performance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.components.GamerXCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    navController: NavController,
    viewModel: PerformanceViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
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
                "Performance Tweaks",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Coming Soon...",
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CpuCoreCard(core: CpuCore) {
    GamerXCard(padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Core ${core.id}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(core.governor, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = core.freq, 
                fontWeight = FontWeight.Medium, 
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}
