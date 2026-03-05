package com.gamerx.manager.feature.spoofer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class Template(
    val id: String,
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val device: String = "",
    val product: String = "",
    val fingerprint: String = ""
)

data class AppConfig(
    val packageName: String,
    var templateId: String? = null
)

class SpooferViewModel : ViewModel() {

    private val CONFIG_PATH = "/data/adb/device_faker/config/config.toml"
    
    // State
    val templates = mutableStateListOf<Template>()
    val appConfigs = mutableStateMapOf<String, AppConfig>()
    val isLoading = mutableStateOf(false)
    val statusText = mutableStateOf("")

    // Current global status (legacy support/display)
    var currentModel = mutableStateOf("Unknown")
    var currentBrand = mutableStateOf("Unknown")

    init {
        loadConfig()
    }

    fun loadConfig() {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val (exit, output) = ShellManager.runCommand("cat $CONFIG_PATH")
            
            if (exit == 0) {
                parseConfig(output)
            } else {
                launch(Dispatchers.Main) { statusText.value = "Failed to load config" }
            }
            launch(Dispatchers.Main) { 
                isLoading.value = false 
                // Fill legacy display props for UI compatibility
                if (templates.isNotEmpty()) {
                    currentModel.value = "Loaded ${templates.size} templates"
                    currentBrand.value = "Loaded ${appConfigs.size} per-app configs"
                }
            }
        }
    }
    
    // Simple Cache for App Icons
    val iconCache = android.util.LruCache<String, android.graphics.Bitmap>(20 * 1024 * 1024) // 20MB


    private suspend fun parseConfig(content: String) {
        val newTemplates = mutableListOf<Template>()
        val newAppConfigs = mutableMapOf<String, AppConfig>()
        
        // Regex for [templates.xxx]
        val templateRegex = Regex("^\\[templates\\.(.+?)\\]")
        // Regex for [apps."xxx"]
        val appRegex = Regex("^\\[apps\\.\"(.+?)\"\\]")
        
        var currentSection = ""
        var currentId = ""
        var tempProps = mutableMapOf<String, String>()

        fun commitSection() {
            if (currentSection == "template" && currentId.isNotEmpty()) {
                newTemplates.add(Template(
                    id = currentId,
                    manufacturer = tempProps["manufacturer"] ?: "",
                    brand = tempProps["brand"] ?: "",
                    model = tempProps["model"] ?: "",
                    device = tempProps["device"] ?: "",
                    product = tempProps["product"] ?: "",
                    fingerprint = tempProps["fingerprint"] ?: ""
                ))
            } else if (currentSection == "app" && currentId.isNotEmpty()) {
                newAppConfigs[currentId] = AppConfig(currentId, tempProps["template"])
            }
            tempProps.clear()
        }

        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) return@forEach

            val templateMatch = templateRegex.find(trimmed)
            val appMatch = appRegex.find(trimmed)

            if (templateMatch != null) {
                commitSection()
                currentSection = "template"
                currentId = templateMatch.groupValues[1]
            } else if (appMatch != null) {
                commitSection()
                currentSection = "app"
                currentId = appMatch.groupValues[1]
            } else if (trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    // Remove quotes from value: "OnePlus" -> OnePlus
                    val value = parts[1].trim().removeSurrounding("\"")
                    tempProps[key] = value
                }
            }
        }
        commitSection() // Commit last block

        viewModelScope.launch(Dispatchers.Main) {
            templates.clear()
            templates.addAll(newTemplates)
            appConfigs.clear()
            appConfigs.putAll(newAppConfigs)
        }
    }

    fun saveAppConfig(packageName: String, templateId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) { isLoading.value = true }
             
            // Read current file
            val (exit, output) = ShellManager.runCommand("cat $CONFIG_PATH")
            if (exit != 0) {
                 launch(Dispatchers.Main) { isLoading.value = false }
                 return@launch
            }

            val lines = output.lines().toMutableList()
            val newLines = mutableListOf<String>()
            
            // Logic: remove existing block for this app if exists, then append new one
            var skipMode = false
            val appHeaderStart = "[apps.\"$packageName\"]"
            
            for (line in lines) {
                if (line.trim() == appHeaderStart) {
                    skipMode = true
                    continue
                }
                
                if (skipMode) {
                    // Stop skipping if we hit next section
                    if (line.trim().startsWith("[")) {
                        skipMode = false
                        newLines.add(line)
                    }
                } else {
                    newLines.add(line)
                }
            }
            
            // Append new block at end if templateId is set
            if (templateId != null) {
                newLines.add("")
                newLines.add(appHeaderStart)
                newLines.add("template = \"$templateId\"")
            }
            
            // Write back
            val newContent = newLines.joinToString("\n")
            // Use tmp file to handle special chars safely
            ShellManager.runCommand("echo \"${newContent.replace("\"", "\\\"").replace("$", "\\$")}\" > /data/local/tmp/config_gen.toml")
            ShellManager.runCommand("cp /data/local/tmp/config_gen.toml $CONFIG_PATH")
            ShellManager.runCommand("chmod 666 $CONFIG_PATH") 
            
            // Refresh
            loadConfig()
            
            launch(Dispatchers.Main) {
                statusText.value = "Config Updated for $packageName"
                isLoading.value = false
            }
        }
    }
    fun addManualTemplate(t: Template) {
        viewModelScope.launch(Dispatchers.IO) {
             launch(Dispatchers.Main) { isLoading.value = true }
             
             val block = """
                 
                 [templates.${t.id}]
                 manufacturer = "${t.manufacturer}"
                 brand = "${t.brand}"
                 model = "${t.model}"
                 device = "${t.device}"
                 product = "${t.product}"
                 fingerprint = "${t.fingerprint}"
             """.trimIndent()
             
             // Append to file
             // We use echo but escape quotes just in case, though fields shouldn't have quotes usually
             ShellManager.runCommand("echo \"$block\" >> $CONFIG_PATH")
             
             loadConfig()
             launch(Dispatchers.Main) { 
                 isLoading.value = false
                 statusText.value = "Template ${t.id} added"
             }
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ShellManager.runCommand("am force-stop $packageName")
            launch(Dispatchers.Main) { statusText.value = "Force stopped $packageName" }
        }
    }
}
