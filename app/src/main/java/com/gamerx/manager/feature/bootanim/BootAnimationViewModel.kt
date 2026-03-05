package com.gamerx.manager.feature.bootanim

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class BootAnim(val name: String, val path: String, val previewPath: String = "", val width: Int = 1080, val height: Int = 1920)

class BootAnimViewModel : ViewModel() {
    companion object {
        val anims = mutableStateListOf<BootAnim>()
        val installedAnim = androidx.compose.runtime.mutableStateOf("")
    }

    val isLoading = androidx.compose.runtime.mutableStateOf(false)

    val statusText = androidx.compose.runtime.mutableStateOf("")

    init {
        // needs cacheDir, delayed init
    }

    fun loadAnims(cacheDir: java.io.File) {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modulePath = com.gamerx.manager.core.ModulePaths.getModulePath()
                val animSource = "$modulePath/bootanimations"
                
                // List directories (variants) safely
                val (exit, output) = ShellManager.runCommand("ls -F $animSource | grep /")
                
                val newAnims = mutableListOf<BootAnim>()
                if (exit == 0 && output.isNotBlank()) {
                    output.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                        try {
                            val name = line.trim().dropLast(1)
                            val animRoot = "$animSource/$name"
                            
                            val (fExit, fOutput) = ShellManager.runCommand("ls \"$animRoot\"")
                            val files = if (fExit == 0) fOutput else ""
                            
                            val hasZip = files.contains("bootanimation.zip")
                            var localPreviewPath = "" 
                            var width = 1080
                            var height = 1920
                            
                            if (hasZip) {
                                val zipPath = "$animRoot/bootanimation.zip"
                                
                                // Parse desc.txt for Resolution
                                try {
                                    val (dExit, dOut) = ShellManager.runCommand("unzip -p \"$zipPath\" desc.txt | head -n 1")
                                    if (dExit == 0 && dOut.isNotBlank()) {
                                        val parts = dOut.trim().split(Regex("\\s+"))
                                        if (parts.size >= 2) {
                                            width = parts[0].toIntOrNull() ?: 1080
                                            height = parts[1].toIntOrNull() ?: 1920
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Default to 1080p
                                }

                                val frameDir = java.io.File(cacheDir, "frames_${name}")
                                if (!frameDir.exists()) frameDir.mkdirs()
                                
                                // Check if we already have frames (safely recursive)
                                val hasFrames = frameDir.walk().any { it.isFile && it.extension == "png" && it.name != "thumbnail.png" }
                                
                                if (!hasFrames) {
                                    // Extract frames
                                    // Get list of part0 pngs
                                    val (lExit, lOut) = ShellManager.runCommand("unzip -l \"$zipPath\" | grep \"part0/.*.png\" | sort | head -n 60")
                                    if (lExit == 0 && lOut.isNotBlank()) {
                                         val filesToExtract = lOut.trim().lines().mapNotNull { l ->
                                            l.split(Regex("\\s+")).lastOrNull()
                                         }.joinToString(" ")
                                         
                                         if (filesToExtract.isNotBlank()) {
                                             ShellManager.runCommand("unzip -o \"$zipPath\" $filesToExtract -d \"${frameDir.absolutePath}\"")
                                         }
                                    }
                                }
                                
                                // Generate Middle Frame Thumbnail
                                val allPngs = frameDir.walk()
                                    .filter { it.isFile && it.extension == "png" && it.name != "thumbnail.png" }
                                    .sortedBy { it.name }
                                    .toList()
                                    
                                if (allPngs.isNotEmpty()) {
                                    val middleIndex = allPngs.size / 2
                                    val middleFrame = allPngs[middleIndex]
                                    val thumbFile = java.io.File(frameDir, "thumbnail.png")
                                    if (!thumbFile.exists()) {
                                        middleFrame.copyTo(thumbFile)
                                    }
                                    localPreviewPath = frameDir.absolutePath
                                }
                            }
                            
                            if (hasZip) {
                                val path = "$animRoot/bootanimation.zip"
                                newAnims.add(BootAnim(name, path, localPreviewPath, width, height))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                launch(Dispatchers.Main) {
                    anims.clear()
                    anims.addAll(newAnims)
                    isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    isLoading.value = false
                }
            }
        }
    }
    
    // ... importBootAnim and deleteBootAnim remain same ...
    
    fun installBootAnim(sourcePath: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val modulePath = com.gamerx.manager.core.ModulePaths.getModulePath()
                
                // Install logic
                ShellManager.runCommand("mkdir -p $modulePath/system/media")
                
                // Check for product path support
                val (checkExit, checkOutput) = ShellManager.runCommand("if [ -f /system/product/media/bootanimation.zip ]; then echo 'true'; else echo 'false'; fi")
                
                // Determine Base Path
                val basePath = if (checkExit == 0 && checkOutput.trim() == "true") {
                     ShellManager.runCommand("mkdir -p $modulePath/system/product/media")
                     "$modulePath/system/product/media"
                } else {
                     ShellManager.runCommand("mkdir -p $modulePath/system/media")
                     "$modulePath/system/media"
                }
                
                val targetNormal = "$basePath/bootanimation.zip"
                val targetDark = "$basePath/bootanimation-dark.zip"
                
                // Copy to BOTH (Pro Feature: Dark Mode Support)
                val cmd = "cp \"$sourcePath\" \"$targetNormal\" && cp \"$sourcePath\" \"$targetDark\" && chmod 644 \"$targetNormal\" && chmod 644 \"$targetDark\" && chcon u:object_r:system_file:s0 \"$targetNormal\" && chcon u:object_r:system_file:s0 \"$targetDark\""
                val (exitCode, output) = ShellManager.runCommand(cmd)
                
                launch(Dispatchers.Main) {
                    if (exitCode == 0) {
                        statusText.value = "Bootanimation Applied! Reboot required."
                        installedAnim.value = sourcePath 
                        
                        // Persistence: Save to SharedPrefs/DataStore (Simulated here via file marker or just local state for now)
                        // In a real app, use DataStore. For now, we rely on UI state passing.
                    } else {
                        statusText.value = "Error: $output"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) { statusText.value = "Error: ${e.message}" }
            }
        }
    }
    
    fun rebootSystem() {
        viewModelScope.launch {
            ShellManager.runCommand("reboot", asRoot = true)
        }
    }

    // Keep import/delete as are for now
    fun importBootAnim(uri: android.net.Uri, context: android.content.Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                launch(Dispatchers.Main) { statusText.value = "Importing..." }
                val cleanName = name.replace(" ", "_").replace(Regex("[^A-Za-z0-9_]"), "")
                val modulePath = com.gamerx.manager.core.ModulePaths.getModulePath()
                val animSourcePath = "$modulePath/bootanimations"
                val targetDir = "$animSourcePath/$cleanName"
                ShellManager.runCommand("mkdir -p \"$targetDir\"")
                
                val contentResolver = context.contentResolver
                val tempFile = java.io.File(context.cacheDir, "import_temp.zip")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                ShellManager.runCommand("cp \"${tempFile.absolutePath}\" \"$targetDir/bootanimation.zip\"")
                ShellManager.runCommand("chmod -R 777 \"$targetDir\"") 
                
                // Trigger reload to generate preview
                loadAnims(context.cacheDir)
                launch(Dispatchers.Main) { statusText.value = "Imported $cleanName" }
                
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) { statusText.value = "Import Failed: ${e.message}" }
            }
        }
    }
    
    fun deleteBootAnim(anim: BootAnim, cacheDir: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
             val modulePath = kotlinx.coroutines.runBlocking { com.gamerx.manager.core.ModulePaths.getModulePath() }
             val animSourcePath = "$modulePath/bootanimations"
             val dirToRemove = "$animSourcePath/${anim.name}"
             ShellManager.runCommand("rm -rf \"$dirToRemove\"")
             loadAnims(cacheDir)
             launch(Dispatchers.Main) { statusText.value = "Deleted ${anim.name}" }
        }
    }
}
