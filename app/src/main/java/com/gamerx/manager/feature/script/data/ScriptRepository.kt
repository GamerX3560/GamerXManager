package com.gamerx.manager.feature.script.data

import android.content.Context
import com.gamerx.manager.feature.script.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object ScriptRepository {

    private const val SCRIPTS_DIR_NAME = "scripts"
    private const val TILE_BINDINGS_FILE = "tile_bindings.json"
    
    private var scriptsDir: File? = null
    private var appContext: Context? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        scriptsDir = File(context.filesDir, SCRIPTS_DIR_NAME)
        if (!scriptsDir!!.exists()) {
            scriptsDir!!.mkdirs()
        }
    }

    // ==================== SCRIPT OPERATIONS ====================

    suspend fun getScripts(): List<ScriptManifest> = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext emptyList()
        val scripts = mutableListOf<ScriptManifest>()

        dir.listFiles()?.forEach { scriptDir ->
            if (scriptDir.isDirectory) {
                val manifestFile = File(scriptDir, "manifest.json")
                if (manifestFile.exists()) {
                    try {
                        val manifest = json.decodeFromString<ScriptManifest>(manifestFile.readText())
                        if (manifest.id == scriptDir.name) {
                            scripts.add(manifest)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return@withContext scripts.sortedBy { it.identity.name.lowercase() }
    }

    suspend fun getScript(id: String): ScriptManifest? = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext null
        val manifestFile = File(dir, "$id/manifest.json")
        if (!manifestFile.exists()) return@withContext null
        
        try {
            json.decodeFromString<ScriptManifest>(manifestFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getScriptFile(id: String, fileName: String = "logic.sh"): File? = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext null
        val file = File(dir, "$id/$fileName")
        return@withContext if (file.exists()) file else null
    }

    suspend fun deleteScript(id: String): Boolean = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext false
        val scriptDir = File(dir, id)
        if (!scriptDir.exists()) return@withContext false
        
        // Remove any tile bindings for this script
        val bindings = getTileBindings()
        bindings.bindings.entries.removeIf { it.value.scriptId == id }
        saveTileBindings(bindings)
        
        scriptDir.deleteRecursively()
    }

    suspend fun updateManifest(manifest: ScriptManifest): Boolean = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext false
        val manifestFile = File(dir, "${manifest.id}/manifest.json")
        if (!manifestFile.parentFile?.exists()!!) return@withContext false
        
        try {
            manifestFile.writeText(json.encodeToString(ScriptManifest.serializer(), manifest))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getScriptDirectory(id: String): File? {
        val dir = scriptsDir ?: return null
        return File(dir, id).takeIf { it.exists() }
    }

    fun getAssetFile(id: String, path: String): File? {
        val dir = scriptsDir ?: return null
        val file = File(dir, "$id/$path")
        return if (file.exists()) file else null
    }

    // ==================== TILE BINDING OPERATIONS ====================

    suspend fun getTileBindings(): TileBindingsConfig = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext TileBindingsConfig()
        val file = File(dir, TILE_BINDINGS_FILE)
        
        if (!file.exists()) return@withContext TileBindingsConfig()
        
        try {
            json.decodeFromString<TileBindingsConfig>(file.readText())
        } catch (e: Exception) {
            TileBindingsConfig()
        }
    }

    suspend fun saveTileBindings(config: TileBindingsConfig) = withContext(Dispatchers.IO) {
        val dir = scriptsDir ?: return@withContext
        val file = File(dir, TILE_BINDINGS_FILE)
        file.writeText(json.encodeToString(TileBindingsConfig.serializer(), config))
    }

    suspend fun getBindingForSlot(slotIndex: Int): TileBinding? = withContext(Dispatchers.IO) {
        getTileBindings().getBinding(slotIndex)
    }

    suspend fun setBindingForSlot(slotIndex: Int, binding: TileBinding) = withContext(Dispatchers.IO) {
        val config = getTileBindings()
        config.setBinding(slotIndex, binding)
        saveTileBindings(config)
    }

    suspend fun removeBindingForSlot(slotIndex: Int) = withContext(Dispatchers.IO) {
        val config = getTileBindings()
        config.removeBinding(slotIndex)
        saveTileBindings(config)
    }

    // ==================== UTILITY ====================
    
    fun getScriptsPath(): File? = scriptsDir
    
    // ==================== TILE MODE OPERATIONS ====================
    
    suspend fun updateToggleState(slotIndex: Int, isOn: Boolean) = withContext(Dispatchers.IO) {
        val config = getTileBindings()
        config.updateToggleState(slotIndex, isOn)
        saveTileBindings(config)
    }
    
    suspend fun cycleQuickSelect(slotIndex: Int): String? = withContext(Dispatchers.IO) {
        val config = getTileBindings()
        val nextValue = config.cycleQuickSelect(slotIndex)
        saveTileBindings(config)
        return@withContext nextValue
    }
}
