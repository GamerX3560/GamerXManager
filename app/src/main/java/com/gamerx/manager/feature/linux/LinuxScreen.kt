package com.gamerx.manager.feature.linux

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gamerx.manager.core.ShellManager
import com.gamerx.manager.ui.components.GamerXCard
import com.gamerx.manager.ui.theme.ThemeManager
import com.gamerx.manager.worker.LinuxInstallerWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinuxViewModel : ViewModel() {
    
    val isInstalled = mutableStateOf(false)
    val isRunning = mutableStateOf(false)
    
    // Installation State
    val isInstalling = mutableStateOf(false)
    val installProgress = mutableStateOf(0)
    val installStatus = mutableStateOf("Ready to install")
    val installSpeed = mutableStateOf("") // e.g. "3.5 MB/s"
    
    val outputLog = mutableStateOf("")

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check Install: Look for os-release in new path
            val checkRootfs = ShellManager.runCommand("if [ -f \"${LinuxConstants.ROOTFS_PATH}/etc/os-release\" ]; then echo 'yes'; else echo 'no'; fi")
            val rootfsExists = checkRootfs.second.trim() == "yes"
            
            withContext(Dispatchers.Main) {
                isInstalled.value = rootfsExists
            }

            if (rootfsExists) {
                // Check Running: More robust check for Xvnc
                val checkRunning = ShellManager.runCommand("pgrep -f 'Xvnc :1'")
                val checkVnc = ShellManager.runCommand("pgrep -f vncserver")
                
                withContext(Dispatchers.Main) {
                    isRunning.value = checkRunning.second.isNotEmpty() || checkVnc.second.isNotEmpty()
                    installStatus.value = if (isRunning.value) "Active (Running)" else "Stopped"
                }
            } else {
                 withContext(Dispatchers.Main) {
                     installStatus.value = "Not Installed"
                 }
            }
        }
    }
    
    fun installLinux(context: android.content.Context) {
        isInstalling.value = true
        installStatus.value = "Starting Downloader..."
        installProgress.value = 0
        
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<LinuxInstallerWorker>()
            .addTag(LinuxConstants.WORK_TAG)
            .build()
            
        workManager.enqueue(request)
    }

    fun startLinux() {
        viewModelScope.launch(Dispatchers.IO) {
            log("Starting Linux GUI (vncserver)...")
            withContext(Dispatchers.Main) { installStatus.value = "Starting..." }
            
            // Call the start_gui.sh script which is now in BIN_PATH
            val result = ShellManager.runCommand("sh ${LinuxConstants.BIN_PATH}/start_gui.sh")
            log(result.second)
            
            delay(3000) // Wait for spin up
            checkStatus()
        }
    }

    fun stopLinux() {
        viewModelScope.launch(Dispatchers.IO) {
            log("Stopping Linux environment...")
            withContext(Dispatchers.Main) { installStatus.value = "Stopping..." }
            
            val result = ShellManager.runCommand("sh ${LinuxConstants.BIN_PATH}/stop_arch.sh")
            log(result.second)
            
            delay(2000)
            checkStatus()
        }
    }

    private fun log(msg: String) {
        val current = outputLog.value
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newMsg = "[$time] $msg"
        val newLog = if (current.length > 5000) current.takeLast(5000) else current
        outputLog.value = newLog + "\n$newMsg"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinuxScreen(navController: NavController, viewModel: LinuxViewModel = viewModel()) {
    val context = LocalContext.current
    
    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* No-op */ }
    )

    // Observe WorkManager for Installation Progress
    val workInfos = WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(LinuxConstants.WORK_TAG)
        .observeAsState()

    LaunchedEffect(workInfos.value) {
        val info = workInfos.value?.firstOrNull()
        if (info != null) {
            if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                viewModel.isInstalling.value = true
                viewModel.installProgress.value = info.progress.getInt(LinuxConstants.KEY_PROGRESS, 0)
                viewModel.installStatus.value = info.progress.getString(LinuxConstants.KEY_STATUS) ?: "Installing..."
            } else if (info.state == WorkInfo.State.SUCCEEDED) {
                viewModel.isInstalling.value = false
                viewModel.installProgress.value = 100
                viewModel.installStatus.value = "Installation Complete"
                viewModel.checkStatus()
            } else if (info.state == WorkInfo.State.FAILED) {
                viewModel.isInstalling.value = false
                val error = info.outputData.getString(LinuxConstants.KEY_ERROR) ?: "Unknown Error"
                viewModel.installStatus.value = "Failed: $error"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GamerX Linux", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Installation Status / Dashboard
            if (!viewModel.isInstalled.value && !viewModel.isInstalling.value) {
                // Case: NOT INSTALLED
                GamerXCard(modifier = Modifier.fillMaxWidth(), padding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(64.dp), tint = ThemeManager.accentColor.value)
                        Spacer(Modifier.height(16.dp))
                        Text("Linux Environment Required", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The GamerX Linux RootFS is missing. You need to download and install it (~1.8GB) to use desktop features.\nInstalls to: ${LinuxConstants.LINUX_ROOT}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                viewModel.installLinux(context) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor.value),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download & Install", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (viewModel.isInstalling.value) {
                // Case: INSTALLING
                 GamerXCard(modifier = Modifier.fillMaxWidth(), padding = 24.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = viewModel.installProgress.value / 100f,
                            color = ThemeManager.accentColor.value,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Installing Linux Environment...", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = viewModel.installStatus.value,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${viewModel.installProgress.value}%", color = ThemeManager.accentColor.value, fontWeight = FontWeight.Bold)
                    }
                 }
            } else {
                // Case: INSTALLED & READY
                DashboardView(viewModel, context)
            }
            
            Spacer(Modifier.height(24.dp))

            // 3. Log Output (Always visible if exists)
            if (viewModel.outputLog.value.isNotEmpty()) {
                Text("System Log", style = MaterialTheme.typography.titleMedium, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                Card(
                     colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                     modifier = Modifier.fillMaxWidth().height(200.dp)
                ) {
                     Text(
                        text = viewModel.outputLog.value,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardView(viewModel: LinuxViewModel, context: android.content.Context) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Status Circle
        GamerXCard(modifier = Modifier.fillMaxWidth(), padding = 24.dp) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Box(
                     modifier = Modifier
                         .size(100.dp)
                         .background(
                             color = if (viewModel.isRunning.value) Color(0xFF00C853) else Color(0xFFD32F2F),
                             shape = CircleShape
                         )
                         .padding(4.dp)
                         .background(Color.Black.copy(alpha=0.2f), CircleShape), // Inner shadow rim
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         imageVector = Icons.Default.Computer,
                         contentDescription = null,
                         modifier = Modifier.size(50.dp),
                         tint = Color.White
                     )
                 }
                 
                 Spacer(Modifier.height(16.dp))
                 
                 Text(
                     text = if (viewModel.isRunning.value) "System Online" else "System Offline",
                     style = MaterialTheme.typography.headlineMedium,
                     fontWeight = FontWeight.Bold,
                     color = Color.White
                 )
                 
                 Text(
                     text = if (viewModel.isRunning.value) "VNC Server: 127.0.0.1:5901" else "Linux RootFS Ready",
                     style = MaterialTheme.typography.bodyLarge,
                     color = Color.LightGray
                 )
             }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Actions Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
             // Main Toggle
             Button(
                onClick = { if (viewModel.isRunning.value) viewModel.stopLinux() else viewModel.startLinux() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isRunning.value) Color(0xFFD32F2F) else ThemeManager.accentColor.value
                ),
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (viewModel.isRunning.value) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(if (viewModel.isRunning.value) "STOP" else "START", fontWeight = FontWeight.Bold)
                }
            }
            
            // Terminal
             Button(
                onClick = { launchTerminal(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("SHELL", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // VNC
             Button(
                onClick = { launchVnc(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)), // Orange for VNC
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.isRunning.value
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Monitor, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("VNC APP", fontWeight = FontWeight.Bold)
                }
            }
            
            // Web / NoVNC
             Button(
                onClick = { launchWebVnc(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), // Blue for Web
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.isRunning.value
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("WEB UI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun launchTerminal(context: android.content.Context) {
    val packageNames = listOf("com.termux", "com.gamerx.terminal", "com.offsec.nethunter")
    var intent: android.content.Intent? = null
    for (pkg in packageNames) {
        intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) break
    }

    if (intent != null) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Command", "su -c sh ${LinuxConstants.BIN_PATH}/enter_arch.sh")
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "Command copied! Paste in terminal.", android.widget.Toast.LENGTH_LONG).show()
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        android.widget.Toast.makeText(context, "No Terminal App Found!", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun launchVnc(context: android.content.Context) {
    try {
        val uri = android.net.Uri.parse("vnc://127.0.0.1:5901")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
         android.widget.Toast.makeText(context, "No VNC Viewer found. Install RealVNC or similar.", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun launchWebVnc(context: android.content.Context) {
    val uri = android.net.Uri.parse("http://127.0.0.1:6080/vnc.html")
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
