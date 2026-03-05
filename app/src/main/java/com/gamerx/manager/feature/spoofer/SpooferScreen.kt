package com.gamerx.manager.feature.spoofer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamerx.manager.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SpooferScreen(navController: NavController) {
    val viewModel: SpooferViewModel = viewModel()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    // Fetch apps (Strings only to avoid lag)
    var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Feedback Logic
    LaunchedEffect(viewModel.statusText.value) {
        if (viewModel.statusText.value.isNotEmpty()) {
            android.widget.Toast.makeText(context, viewModel.statusText.value, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.statusText.value = "" // Reset
        }
    }
    
    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
             withContext(Dispatchers.IO) {
                 try {
                    val pm = context.packageManager
                    val apps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                        .filter { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // User apps only
                        .map { 
                            AppItem(
                                it.packageName,
                                it.applicationInfo.loadLabel(pm).toString()
                            )
                        }
                        .sortedBy { it.label }
                    withContext(Dispatchers.Main) {
                        installedApps = apps
                    }
                 } catch(e: Exception) {
                     e.printStackTrace()
                 }
             }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ThemeManager.accentColor.value,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 90.dp) // Lift above Nav Bar
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Template")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Device Spoofer Pro",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = ThemeManager.accentColor.value,
                divider = { Divider(color = Color.Gray.copy(alpha = 0.3f)) }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Templates") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Per-App Config") })
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                TemplatesList(viewModel)
            } else {
                AppList(viewModel, installedApps)
            }
        }
    }
    
    if (showAddDialog) {
        AddTemplateDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { t -> 
                viewModel.addManualTemplate(t)
                showAddDialog = false 
            }
        )
    }
}

@Composable
fun TemplatesList(viewModel: SpooferViewModel) {
    if (viewModel.isLoading.value) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             CircularProgressIndicator(color = ThemeManager.accentColor.value)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for FAB and Nav Bar
        ) {
            items(viewModel.templates) { template ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeManager.getCardColor()),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(text = template.id, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "${template.manufacturer} ${template.model}", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fingerprint: ${template.fingerprint.take(30)}...", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun AppList(viewModel: SpooferViewModel, apps: List<AppItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(apps) { app ->
            val config = viewModel.appConfigs[app.packageName]
            val hasConfig = config != null
            val currentTemplate = config?.templateId ?: "Default"
            var expanded by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeManager.getCardColor()),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppIcon(app.packageName, Modifier.size(40.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(text = app.packageName, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Box {
                            Button(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if(hasConfig) ThemeManager.accentColor.value else Color.Gray.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(currentTemplate, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default (None)") },
                                    onClick = { 
                                        viewModel.saveAppConfig(app.packageName, null)
                                        expanded = false 
                                    }
                                )
                                viewModel.templates.forEach { temp ->
                                    DropdownMenuItem(
                                        text = { Text(temp.id) },
                                        onClick = { 
                                            viewModel.saveAppConfig(app.packageName, temp.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    if (hasConfig) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.forceStopApp(app.packageName) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.7f)),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Force Stop to Apply", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SpooferViewModel = viewModel()
    
    // Use produceState to load icon asynchronously but check cache first
    val bitmapState = produceState<Bitmap?>(initialValue = viewModel.iconCache.get(packageName), key1 = packageName) {
        // Double check cache in coroutine to be safe/fast
        var cached = viewModel.iconCache.get(packageName)
        if (cached != null) {
            value = cached
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bitmap = drawableToBitmap(drawable)
                    viewModel.iconCache.put(packageName, bitmap)
                    withContext(Dispatchers.Main) {
                        value = bitmap
                    }
                } catch (e: Exception) {
                    // e.printStackTrace() // Ignore errors for smoother scroll
                }
            }
        }
    }
    
    if (bitmapState.value != null) {
        Image(
            bitmap = bitmapState.value!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Gray.copy(0.3f)))
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun AddTemplateDialog(onDismiss: () -> Unit, onAdd: (Template) -> Unit) {
    var id by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Template") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = ThemeManager.accentColor.value,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = ThemeManager.accentColor.value,
                    focusedBorderColor = ThemeManager.accentColor.value,
                    unfocusedBorderColor = Color.Gray
                )
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    item { OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("ID (e.g. pixel8)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = { Text("Manufacturer") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = device, onValueChange = { device = it }, label = { Text("Device Code") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = product, onValueChange = { product = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors) }
                    item { OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, label = { Text("Build Fingerprint") }, modifier = Modifier.fillMaxWidth().height(80.dp), colors = colors) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (id.isNotEmpty()) {
                    onAdd(Template(id, manufacturer, brand, model, device, product, fingerprint))
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class AppItem(val packageName: String, val label: String)
