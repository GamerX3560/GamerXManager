package com.gamerx.manager.feature.script.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.gamerx.manager.feature.script.data.ScriptRepository
import com.gamerx.manager.feature.script.data.ScriptStateManager
import com.gamerx.manager.feature.script.logic.ScriptExecutor
import com.gamerx.manager.feature.script.model.TileBinding
import com.gamerx.manager.feature.script.model.TileMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Base Quick Settings Tile Service for Script Execution
 * 
 * Supports:
 * - EXECUTE: Single execution
 * - TOGGLE: On/Off state with persistent state
 * - POPUP: Launch options dialog (TODO: TilePopupActivity)
 * - QUICK_SELECT: Cycle through preset values
 */
abstract class BaseScriptTileService(private val slotIndex: Int) : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        val tile = qsTile ?: return
        
        serviceScope.launch {
            try {
                ScriptRepository.init(applicationContext)
                val binding = ScriptRepository.getBindingForSlot(slotIndex)
                
                launch(Dispatchers.Main) {
                    updateTileState(tile, binding)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.label = "Error"
                    tile.updateTile()
                }
            }
        }
    }

    override fun onClick() {
        val tile = qsTile ?: return
        
        serviceScope.launch {
            try {
                ScriptRepository.init(applicationContext)
                val binding = ScriptRepository.getBindingForSlot(slotIndex)
                
                if (binding == null) {
                    showToast("No script bound to Slot $slotIndex")
                    return@launch
                }
                
                when (binding.mode) {
                    TileMode.EXECUTE -> handleExecuteMode(binding, tile)
                    TileMode.TOGGLE -> handleToggleMode(binding, tile)
                    TileMode.POPUP -> handlePopupMode(binding, tile)
                    TileMode.QUICK_SELECT -> handleQuickSelectMode(binding, tile)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Execution failed: ${e.message}")
                launch(Dispatchers.Main) {
                    tile.state = Tile.STATE_INACTIVE
                    tile.updateTile()
                }
            }
        }
    }

    private suspend fun handleExecuteMode(binding: TileBinding, tile: Tile) {
        // Set active state during execution
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
        
        executeScript(binding, tile)
        resetTile(tile)
    }

    private suspend fun handleToggleMode(binding: TileBinding, tile: Tile) {
        val newState = !binding.toggleState
        
        // Update toggle state
        ScriptRepository.updateToggleState(slotIndex, newState)
        
        // Update tile UI
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            tile.state = if (newState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (newState) "On" else "Off"
            tile.updateTile()
        }
        
        // Execute with toggle state as input
        val manifest = ScriptRepository.getScript(binding.scriptId) ?: return
        val scriptFile = ScriptRepository.getScriptFile(binding.scriptId, manifest.execution.entryPoint) ?: return
        
        val savedState = ScriptStateManager.loadState(applicationContext, binding.scriptId)
        val allInputs = savedState.toMutableMap()
        allInputs["toggle_state"] = newState.toString()
        binding.profile.inputs.forEach { (k, v) -> allInputs[k] = v }
        
        val jsonInputs = buildJsonObject {
            allInputs.forEach { (key, value) ->
                put(key, JsonPrimitive(value.toString()))
            }
        }
        
        val result = ScriptExecutor.executeScript(manifest, scriptFile, jsonInputs)
        
        val statusIcon = if (result.exitCode == 0) "✓" else "✗"
        showToast("$statusIcon ${binding.label} ${if (newState) "On" else "Off"}")
    }

    private suspend fun handlePopupMode(binding: TileBinding, tile: Tile) {
        showToast("Opening ${binding.label}...")
        
        // Launch popup activity
        try {
            val intent = Intent("com.gamerx.manager.TILE_POPUP").apply {
                putExtra("script_id", binding.scriptId)
                putExtra("slot_index", slotIndex)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setPackage(packageName)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+: Must use PendingIntent version
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this@BaseScriptTileService, 
                    slotIndex,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            // Fallback to execute
            handleExecuteMode(binding, tile)
        }
    }

    private suspend fun handleQuickSelectMode(binding: TileBinding, tile: Tile) {
        if (binding.quickSelectOptions.isEmpty()) {
            showToast("No quick select options configured")
            return
        }
        
        // Cycle to next option
        val nextValue = ScriptRepository.cycleQuickSelect(slotIndex)
        
        if (nextValue != null) {
            // Update tile subtitle with current value
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = nextValue
                tile.updateTile()
            }
            
            // Execute with selected value
            val manifest = ScriptRepository.getScript(binding.scriptId) ?: return
            val scriptFile = ScriptRepository.getScriptFile(binding.scriptId, manifest.execution.entryPoint) ?: return
            
            val savedState = ScriptStateManager.loadState(applicationContext, binding.scriptId)
            val allInputs = savedState.toMutableMap()
            allInputs["quick_select_value"] = nextValue
            binding.profile.inputs.forEach { (k, v) -> allInputs[k] = v }
            
            val jsonInputs = buildJsonObject {
                allInputs.forEach { (key, value) ->
                    put(key, JsonPrimitive(value.toString()))
                }
            }
            
            val result = ScriptExecutor.executeScript(manifest, scriptFile, jsonInputs)
            
            showToast("${binding.label}: $nextValue")
            
            // Keep tile active briefly then reset
            kotlinx.coroutines.delay(1000)
            resetTile(tile)
        }
    }

    private suspend fun executeScript(binding: TileBinding, tile: Tile) {
        val manifest = ScriptRepository.getScript(binding.scriptId)
        
        if (manifest == null) {
            showToast("Script not found")
            resetTile(tile)
            return
        }
        
        val scriptFile = ScriptRepository.getScriptFile(
            binding.scriptId, 
            manifest.execution.entryPoint
        )
        
        if (scriptFile == null || !scriptFile.exists()) {
            showToast("Script file missing: ${manifest.execution.entryPoint}")
            resetTile(tile)
            return
        }
        
        // Build inputs JSON
        val savedState = ScriptStateManager.loadState(applicationContext, binding.scriptId)
        
        // Merge binding profile inputs with saved state
        val allInputs = savedState.toMutableMap()
        binding.profile.inputs.forEach { (k, v) -> allInputs[k] = v }
        
        val jsonInputs = buildJsonObject {
            allInputs.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }
        
        // Execute
        val result = ScriptExecutor.executeScript(manifest, scriptFile, jsonInputs)
        
        // Show result
        val message = when {
            result.timedOut -> "⏱️ Timed out: ${manifest.identity.name}"
            result.exitCode == 0 -> "✓ ${manifest.identity.name}"
            else -> "✗ Failed: ${result.stderr.take(50)}"
        }
        
        showToast(message)
    }

    private fun updateTileState(tile: Tile, binding: TileBinding?) {
        if (binding == null) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Script $slotIndex"
            tile.subtitle = "Not configured"
        } else {
            tile.label = binding.label
            
            // Set icon
            val iconResId = getIconResource(binding.iconName)
            if (iconResId != 0) {
                // tile.icon accepts Icon? only if API is high enough, but standard usage is Icon.createWithResource
                tile.icon = Icon.createWithResource(this, iconResId)
            }

            when (binding.mode) {
                TileMode.TOGGLE -> {
                    tile.state = if (binding.toggleState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.subtitle = if (binding.toggleState) "On" else "Off"
                }
                TileMode.QUICK_SELECT -> {
                    tile.state = Tile.STATE_INACTIVE
                    val currentValue = binding.quickSelectOptions.getOrNull(binding.quickSelectIndex) ?: ""
                    tile.subtitle = currentValue.ifEmpty { "Select" }
                }
                TileMode.POPUP -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = "Tap for options"
                }
                else -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = if (binding.profile.isHeadless) "Headless" else "Tap to run"
                }
            }
        }
        tile.updateTile()
    }

    private fun getIconResource(iconName: String): Int {
        // Map all TileIcons names to drawable resources
        // Using built-in Android icons where possible, custom icons where needed
        return when (iconName) {
            // System icons
            "battery", "battery_full" -> com.gamerx.manager.R.drawable.ic_battery_full
            "battery_charging" -> com.gamerx.manager.R.drawable.ic_battery_full
            "wifi" -> android.R.drawable.ic_menu_share
            "bluetooth" -> com.gamerx.manager.R.drawable.ic_bluetooth
            "volume", "volume_off" -> android.R.drawable.ic_lock_silent_mode_off
            "brightness", "wb_sunny", "flashlight" -> com.gamerx.manager.R.drawable.ic_wb_sunny
            "airplane" -> android.R.drawable.ic_menu_send
            "location" -> android.R.drawable.ic_menu_mylocation
            "sync", "refresh" -> android.R.drawable.ic_menu_rotate
            
            // Media icons  
            "play" -> android.R.drawable.ic_media_play
            "pause" -> android.R.drawable.ic_media_pause
            "music" -> android.R.drawable.ic_media_play
            "video", "camera" -> android.R.drawable.ic_menu_camera
            "mic" -> android.R.drawable.ic_btn_speak_now
            
            // Tools icons
            "terminal", "code" -> android.R.drawable.ic_menu_edit
            "settings", "build" -> android.R.drawable.ic_menu_preferences
            "power" -> android.R.drawable.ic_lock_power_off
            "schedule", "timer" -> android.R.drawable.ic_menu_recent_history
            "download" -> android.R.drawable.stat_sys_download
            "upload" -> android.R.drawable.stat_sys_upload
            
            // Custom icons
            "star" -> android.R.drawable.star_big_on
            "favorite" -> android.R.drawable.star_on
            "bolt", "speed", "rocket" -> android.R.drawable.ic_media_ff
            "shield", "verified" -> android.R.drawable.ic_secure
            
            // Device icons
            "memory", "storage" -> android.R.drawable.ic_menu_save
            "phone" -> android.R.drawable.ic_menu_call
            "developer", "gamepad" -> android.R.drawable.ic_menu_manage
            
            // Default fallback - use app launcher icon for unmapped
            else -> com.gamerx.manager.R.mipmap.ic_launcher
        }
    }

    private suspend fun resetTile(tile: Tile) {
        val binding = ScriptRepository.getBindingForSlot(slotIndex)
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            updateTileState(tile, binding)
        }
    }

    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

// ==================== SLOT SERVICES ====================

class ScriptTileService1 : BaseScriptTileService(1)
class ScriptTileService2 : BaseScriptTileService(2)
class ScriptTileService3 : BaseScriptTileService(3)
class ScriptTileService4 : BaseScriptTileService(4)
class ScriptTileService5 : BaseScriptTileService(5)
class ScriptTileService6 : BaseScriptTileService(6)
class ScriptTileService7 : BaseScriptTileService(7)
class ScriptTileService8 : BaseScriptTileService(8)
class ScriptTileService9 : BaseScriptTileService(9)
class ScriptTileService10 : BaseScriptTileService(10)
class ScriptTileService11 : BaseScriptTileService(11)
class ScriptTileService12 : BaseScriptTileService(12)
class ScriptTileService13 : BaseScriptTileService(13)
class ScriptTileService14 : BaseScriptTileService(14)
class ScriptTileService15 : BaseScriptTileService(15)


