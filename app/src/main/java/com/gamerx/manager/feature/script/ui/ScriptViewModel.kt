package com.gamerx.manager.feature.script.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import com.gamerx.manager.feature.script.data.ScriptImporter
import com.gamerx.manager.feature.script.data.ScriptRepository
import com.gamerx.manager.feature.script.data.ScriptStateManager
import com.gamerx.manager.feature.script.logic.ScriptExecutor
import com.gamerx.manager.feature.script.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID

class ScriptViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ==================== STATE ====================
    
    private val _scripts = MutableStateFlow<List<ScriptManifest>>(emptyList())
    val scripts = _scripts.asStateFlow()

    private val _executionOutput = MutableStateFlow<String?>(null)
    val executionOutput = _executionOutput.asStateFlow()
    
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting = _isExecuting.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _tileBindings = MutableStateFlow<TileBindingsConfig>(TileBindingsConfig())
    val tileBindings = _tileBindings.asStateFlow()

    // ==================== INITIALIZATION ====================

    init {
        ScriptRepository.init(application)
        loadScripts()
        loadTileBindings()
    }

    fun loadScripts() {
        viewModelScope.launch {
            _isLoading.value = true
            _scripts.value = ScriptRepository.getScripts()
            _isLoading.value = false
        }
    }
    
    private fun loadTileBindings() {
        viewModelScope.launch {
            _tileBindings.value = ScriptRepository.getTileBindings()
        }
    }

    // ==================== IMPORT/CREATE ====================
    
    fun importScript(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ScriptImporter.importScript(getApplication(), uri)
                showToast("Script Imported Successfully")
                loadScripts()
            } catch (e: Exception) {
                showToast("Import Failed: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createScript(name: String, description: String = "", template: String = "empty") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = UUID.randomUUID().toString()
                
                val manifest = ScriptManifest(
                    id = id,
                    identity = ScriptIdentity(
                        name = name,
                        author = "User",
                        description = description.ifEmpty { "Created via GamerX Manager" }
                    ),
                    capabilities = ScriptCapabilities(
                        rootRequired = true,
                        qsCompatible = true,
                        headlessCapable = true
                    ),
                    ui = UiDefinition(
                        presentation = "sheet",
                        inputs = emptyList()
                    ),
                    execution = ExecutionConfig(
                        engine = "sh",
                        entryPoint = "logic.sh",
                        timeout = 30
                    )
                )
                
                val context = getApplication<Application>()
                val scriptsDir = File(context.filesDir, "scripts")
                val targetDir = File(scriptsDir, id).apply { mkdirs() }
                
                // Write Manifest
                File(targetDir, "manifest.json").writeText(
                    json.encodeToString(ScriptManifest.serializer(), manifest)
                )
                
                // Write Logic Template
                val code = when(template) {
                    "battery" -> """
#!/system/bin/sh
# Battery Spoofer Demo
# Reads input JSON from stdin

# Set battery level
dumpsys battery set level 50

# Output result
echo '{"status": "success", "message": "Battery set to 50%"}'
                    """.trimIndent()
                    
                    "performance" -> """
#!/system/bin/sh
# Performance Booster Demo

# Set CPU governor (run as root)
echo "performance" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null

echo '{"status": "success", "message": "Performance mode enabled"}'
                    """.trimIndent()
                    
                    else -> """
#!/system/bin/sh
# GamerX Script Template
# Input JSON is available via stdin

# Your commands here
echo "Hello from GamerX!"

# Return JSON result
echo '{"status": "success", "message": "Script executed successfully"}'
                    """.trimIndent()
                }
                File(targetDir, "logic.sh").writeText(code)
                
                showToast("Script '$name' Created")
                loadScripts()
                
            } catch (e: Exception) {
                showToast("Creation Failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Create a script and return its ID (for immediate navigation to editor)
     */
    fun createScriptAndGetId(name: String, description: String = ""): String? {
        return try {
            val id = UUID.randomUUID().toString()
            
            val manifest = ScriptManifest(
                id = id,
                identity = ScriptIdentity(
                    name = name,
                    author = "User",
                    description = description.ifEmpty { "Created via GamerX Manager" }
                ),
                capabilities = ScriptCapabilities(
                    rootRequired = true,
                    qsCompatible = true,
                    headlessCapable = true
                ),
                ui = UiDefinition(
                    presentation = "sheet",
                    inputs = emptyList()
                ),
                execution = ExecutionConfig(
                    engine = "sh",
                    entryPoint = "logic.sh",
                    timeout = 30
                )
            )
            
            val context = getApplication<Application>()
            val scriptsDir = File(context.filesDir, "scripts")
            val targetDir = File(scriptsDir, id).apply { mkdirs() }
            
            // Write Manifest
            File(targetDir, "manifest.json").writeText(
                json.encodeToString(ScriptManifest.serializer(), manifest)
            )
            
            // Write empty shell template
            File(targetDir, "logic.sh").writeText("""
#!/system/bin/sh
# ${name}
# Edit this file to add your script logic

echo '{"status": "success", "message": "Script executed"}'
            """.trimIndent())
            
            viewModelScope.launch { loadScripts() }
            id
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get binding for a specific script
     */
    fun getBindingForScript(scriptId: String): TileBinding? {
        return _tileBindings.value.getBindingByScriptId(scriptId)
    }
    
    /**
     * Import scripts from a GitHub repository
     */
    fun importFromRepo(repoUrl: String, scriptPaths: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val scriptsDir = File(context.filesDir, "scripts")
            var successCount = 0
            var failCount = 0
            
            for (path in scriptPaths) {
                val result = com.gamerx.manager.feature.script.data.ScriptRepoImporter.importScriptFromRepo(
                    repoUrl, path, scriptsDir
                )
                result.fold(
                    onSuccess = { successCount++ },
                    onFailure = { failCount++ }
                )
            }
            
            loadScripts()
            
            withContext(Dispatchers.Main) {
                if (failCount == 0) {
                    showToast("Imported $successCount script(s)")
                } else {
                    showToast("Imported $successCount, Failed $failCount")
                }
            }
        }
    }

    // ==================== DELETE/UPDATE ====================

    fun deleteScript(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = ScriptRepository.deleteScript(id)
            if (success) {
                showToast("Script Deleted")
                loadScripts()
                loadTileBindings()
            } else {
                showToast("Delete Failed")
            }
        }
    }

    fun updateScript(id: String, name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val script = ScriptRepository.getScript(id) ?: return@launch
            val updated = script.copy(
                identity = script.identity.copy(
                    name = name,
                    description = description
                )
            )
            val success = ScriptRepository.updateManifest(updated)
            if (success) {
                showToast("Script Updated")
                loadScripts()
            } else {
                showToast("Update Failed")
            }
        }
    }

    // ==================== EXECUTION ====================

    suspend fun loadScriptState(scriptId: String): Map<String, Any> {
        return ScriptStateManager.loadState(getApplication(), scriptId)
    }

    fun executeScript(manifest: ScriptManifest, inputs: Map<String, Any>) {
        viewModelScope.launch {
            _isExecuting.value = true
            _executionOutput.value = "⏳ Preparing execution..."
            
            try {
                // Build input JSON
                val jsonInputs = buildJsonObject {
                    inputs.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, JsonPrimitive(value))
                            is Boolean -> put(key, JsonPrimitive(value))
                            is Number -> put(key, JsonPrimitive(value))
                            is List<*> -> {
                                // Handle multi-select
                                put(key, JsonPrimitive(value.joinToString(",")))
                            }
                        }
                    }
                }
                
                // Save State for persistence
                ScriptStateManager.saveState(getApplication(), manifest.id, inputs)
                
                // Get Script File
                val scriptFile = ScriptRepository.getScriptFile(manifest.id, manifest.execution.entryPoint)
                
                if (scriptFile == null || !scriptFile.exists()) {
                    _executionOutput.value = "❌ Error: Entry point '${manifest.execution.entryPoint}' not found."
                    _isExecuting.value = false
                    return@launch
                }

                _executionOutput.value = "⚡ Executing..."

                // Execute
                val result = ScriptExecutor.executeScript(manifest, scriptFile, jsonInputs)
                
                // Process result
                val outputBuilder = StringBuilder()
                outputBuilder.append("Exit Code: ${result.exitCode}\n")
                
                if (result.stdout.isNotEmpty()) {
                    outputBuilder.append("\n📤 Output:\n${result.stdout}\n")
                }
                
                if (result.stderr.isNotEmpty()) {
                    outputBuilder.append("\n⚠️ Errors:\n${result.stderr}\n")
                }
                
                // Parse and execute actions from output
                try {
                    if (result.stdout.isNotEmpty()) {
                        val root = json.parseToJsonElement(result.stdout).jsonObject
                        
                        val status = root["status"]?.jsonPrimitive?.content ?: "unknown"
                        val message = root["message"]?.jsonPrimitive?.content ?: ""
                        
                        if (message.isNotEmpty()) {
                            outputBuilder.append("\n✅ $message\n")
                        }
                        
                        if (root.containsKey("actions")) {
                            val actions = root["actions"]?.jsonArray
                            outputBuilder.append("\n🔧 Executed Actions:\n")
                            
                            actions?.forEach { actionElement ->
                                val cmd = actionElement.jsonPrimitive.content
                                val cmdResult = ShellManager.runCommand(cmd, asRoot = true)
                                val icon = if (cmdResult.first == 0) "✓" else "✗"
                                outputBuilder.append("  $icon $cmd\n")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Not JSON output, that's fine
                }
                
                _executionOutput.value = outputBuilder.toString()
                
            } catch (e: Exception) {
                _executionOutput.value = "❌ Execution Error: ${e.message}"
                e.printStackTrace()
            } finally {
                _isExecuting.value = false
            }
        }
    }
    
    fun clearExecutionOutput() {
        _executionOutput.value = null
    }

    // ==================== TILE BINDINGS ====================

    fun bindScriptToTile(
        slotIndex: Int, 
        script: ScriptManifest, 
        iconName: String = "play",
        mode: TileMode = TileMode.EXECUTE,
        quickSelectOptions: List<String> = emptyList(),
        isHeadless: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val binding = TileBinding(
                    slotIndex = slotIndex,
                    scriptId = script.id,
                    label = script.identity.name,
                    iconName = iconName,
                    mode = mode,
                    quickSelectOptions = quickSelectOptions,
                    profile = TileProfile(
                        isHeadless = isHeadless
                    )
                )
                
                ScriptRepository.setBindingForSlot(slotIndex, binding)
                loadTileBindings()
                showToast("Bound to Slot $slotIndex")
                
            } catch (e: Exception) {
                showToast("Binding Failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun unbindTile(slotIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            ScriptRepository.removeBindingForSlot(slotIndex)
            loadTileBindings()
            showToast("Slot $slotIndex Unbound")
        }
    }

    // ==================== UTILITY ====================

    private fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
        }
    }
}
