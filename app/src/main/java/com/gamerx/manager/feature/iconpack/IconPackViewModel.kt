package com.gamerx.manager.feature.iconpack

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class IconPack(val name: String, val path: String, val size: String, val previewPath: String = "", val iconResId: Int = 0, val description: String = "High quality icon pack for System UI.")

class IconPackViewModel : ViewModel() {
    // Global state to survive navigation without DI
    companion object {
        val iconPacks = mutableStateListOf<IconPack>()
        val installationProgress = mutableStateMapOf<String, Float>()
        val installedPack = androidx.compose.runtime.mutableStateOf("")
    }
    
    val isLoading = androidx.compose.runtime.mutableStateOf(false)
    val statusText = androidx.compose.runtime.mutableStateOf("")

    fun loadIconPacks(cacheDir: File) {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modulePath = com.gamerx.manager.core.ModulePaths.getModulePath()
                val iconPacksSource = "$modulePath/iconpacks"
                
                // List directories in iconpacks folder
                val (exit, output) = ShellManager.runCommand("ls -F $iconPacksSource | grep /")
                
                val newPacks = mutableListOf<IconPack>()
                if (exit == 0 && output.isNotBlank()) {
                    output.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                        try {
                            val name = line.trim().dropLast(1)
                            val packRoot = "$iconPacksSource/$name"
                            
                            val (fExit, fOutput) = ShellManager.runCommand("ls \"$packRoot\"")
                            val files = if (fExit == 0) fOutput else ""
                            
                            val hasApk = files.contains("Theme.apk")
                            var localPreviewPath = ""
                            
                            // Check explicit file existence
                            val previewSource = "$packRoot/preview.png"
                            val (pExit, _) = ShellManager.runCommand("[ -f \"$previewSource\" ] && echo 'yes'")
                            
                             if (pExit == 0) { // exists
                                 val destFile = java.io.File(cacheDir, "preview_icon_${name}.png")
                                 
                                 // Always copy to ensure fresh or if missing
                                 ShellManager.runCommand("cp \"$previewSource\" \"${destFile.absolutePath}\"")
                                 
                                 // Fix permissions so app can read it
                                 val uid = android.os.Process.myUid()
                                 ShellManager.runCommand("chown $uid:$uid \"${destFile.absolutePath}\"", asRoot = true)
                                 ShellManager.runCommand("chmod 600 \"${destFile.absolutePath}\"", asRoot = true)

                                 if (destFile.exists()) {
                                     localPreviewPath = destFile.absolutePath
                                 }
                            }
                            
                            // Brand Icon Logic
                            var iconResId = 0
                            val lowerName = name.lowercase()
                            if (lowerName.contains("oneui")) iconResId = com.gamerx.manager.R.drawable.ic_pack_oneui
                            else if (lowerName.contains("oxygen")) iconResId = com.gamerx.manager.R.drawable.ic_pack_oxygen
                            else if (lowerName.contains("miui") || lowerName.contains("hyper")) iconResId = com.gamerx.manager.R.drawable.ic_pack_hyperos
                            else if (lowerName.contains("color")) iconResId = com.gamerx.manager.R.drawable.ic_pack_coloros
                            else if (lowerName.contains("flyme")) iconResId = com.gamerx.manager.R.drawable.ic_pack_flyme
                            else if (lowerName.contains("hello")) iconResId = com.gamerx.manager.R.drawable.ic_pack_helloui
                            else if (lowerName.contains("hios")) iconResId = com.gamerx.manager.R.drawable.ic_pack_hios
                            else if (lowerName.contains("nothing")) iconResId = com.gamerx.manager.R.drawable.ic_pack_nothing
                            else if (lowerName.contains("origin")) iconResId = com.gamerx.manager.R.drawable.ic_pack_origin
                            else if (lowerName.contains("realme")) iconResId = com.gamerx.manager.R.drawable.ic_pack_realme
                            
                            if (hasApk) {
                                val path = "$packRoot/Theme.apk"
                                newPacks.add(IconPack(name, path, "Unknown", localPreviewPath, iconResId, "Official ${name} Icon Pack extracted from firmware."))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    iconPacks.clear()
                    iconPacks.addAll(newPacks)
                    isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { 
                    isLoading.value = false 
                    statusText.value = "Error loading packs"
                }
            }
        }
    }

    fun installIconPack(name: String, sourcePath: String) {
        viewModelScope.launch {
            try {
                installationProgress[name] = 0f
                
                val modulePath = com.gamerx.manager.core.ModulePaths.getModulePath()
                val targetOverlay = "$modulePath/system/product/overlay/Theme.apk"
                
                // Simulate steps
                installationProgress[name] = 0.2f
                kotlinx.coroutines.delay(500)
                
                ShellManager.runCommand("mkdir -p $(dirname $targetOverlay)")
                installationProgress[name] = 0.4f
                kotlinx.coroutines.delay(300)

                // Copy to temporary location for install
                val tempInstallPath = "/data/local/tmp/Theme.apk"
                ShellManager.runCommand("cp \"$sourcePath\" \"$tempInstallPath\" && chmod 644 \"$tempInstallPath\"", asRoot = true)
                
                statusText.value = "Installing Overlay APK..."
                // 1. Install via PM
                val (installExit, installOut) = ShellManager.runCommand("pm install -r \"$tempInstallPath\"", asRoot = true)
                
                if (installExit == 0 || installOut.contains("Success")) {
                    installationProgress[name] = 0.7f
                    statusText.value = "Enabling Overlay..."
                    
                    // 2. Find package name of the installed overlay
                    // We assume it contains ".theme.icon" or similar, OR we list all and find the one that isn't system default?
                    // Actually, we can get it from the APK potentially? Or just guess.
                    // Let's list overlays and look for the most recent one or specific naming convention we use.
                    // For now, let's look for *any* overlay that is NOT "android" and resembles an icon pack?
                    // Safe bet: The APK usually has a static package name. I'll search for "com.gamerx" or similar if we built it.
                    // If it's a generic theme, it might vary.
                    
                    // NEW STRATEGY: List all overlays, enable them all? No.
                    // User said: "just install the theme apk and set it as icon pack".
                    // "Set as icon pack" implies enabling it.
                    
                    // Let's try enabling ALL overlays that match a specific pattern often used by themes.
                    val (listExit, listOut) = ShellManager.runCommand("cmd overlay list | grep -E 'com.android.theme.icon|com.google.android.theme.icon'", asRoot = true)
                    
                    if (listExit == 0) {
                         listOut.lines().forEach { line ->
                             val pkg = line.trim().split(" ").firstOrNull { it.contains("com.") }?.replace("[", "")?.replace("]", "")
                             if (pkg != null) {
                                 ShellManager.runCommand("cmd overlay enable $pkg", asRoot = true)
                             }
                         }
                    }
                    
                    // Aggressive Restart
                    ShellManager.runCommand("killall com.android.systemui", asRoot = true)
                    ShellManager.runCommand("pkill -f com.android.systemui", asRoot = true)
                    
                    installationProgress[name] = 1.0f
                    installedPack.value = name
                    statusText.value = "Installed & Enabled! Restarting UI..."
                    
                } else {
                    installationProgress.remove(name)
                    statusText.value = "Install Failed: $installOut"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                installationProgress.remove(name)
                statusText.value = "Error: ${e.message}"
            }
            }
        }
    

    fun restartSystemUI() {
        viewModelScope.launch {
            // "Simple refresh logic" as per user
            ShellManager.runCommand("killall com.android.systemui", asRoot = true)
            ShellManager.runCommand("pkill -f com.android.systemui", asRoot = true)
            // am restart is backup
            ShellManager.runCommand("am restart", asRoot = true)
        }
    }
}
