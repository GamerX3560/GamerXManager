package com.gamerx.manager.feature.script.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tile execution modes
 */
@Serializable
enum class TileMode {
    @SerialName("execute") EXECUTE,        // Single execution (default)
    @SerialName("toggle") TOGGLE,          // On/Off state persists
    @SerialName("popup") POPUP,            // Show options dialog before execution
    @SerialName("quick_select") QUICK_SELECT  // Cycle through preset values
}

/**
 * Represents a Quick Settings tile binding to a script
 */
@Serializable
data class TileBinding(
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("script_id") val scriptId: String,
    val label: String,
    @SerialName("icon_name") val iconName: String = "play",  // Built-in icon name
    val mode: TileMode = TileMode.EXECUTE,
    @SerialName("toggle_state") val toggleState: Boolean = false,  // For TOGGLE mode
    @SerialName("quick_select_options") val quickSelectOptions: List<String> = emptyList(),  // For QUICK_SELECT
    @SerialName("quick_select_index") val quickSelectIndex: Int = 0,
    val profile: TileProfile = TileProfile()
)

/**
 * Execution profile for a tile
 * - Headless: Execute immediately with stored inputs
 * - Interactive: Show dialog to configure inputs before execution
 */
@Serializable
data class TileProfile(
    @SerialName("is_headless") val isHeadless: Boolean = false,
    val inputs: Map<String, String> = emptyMap() // Pre-configured inputs for headless mode
)

/**
 * Container for all tile bindings
 */
@Serializable
data class TileBindingsConfig(
    val bindings: MutableMap<Int, TileBinding> = mutableMapOf()
) {
    fun getBinding(slotIndex: Int): TileBinding? = bindings[slotIndex]
    
    fun setBinding(slotIndex: Int, binding: TileBinding) {
        bindings[slotIndex] = binding.copy(slotIndex = slotIndex)
    }
    
    fun removeBinding(slotIndex: Int) {
        bindings.remove(slotIndex)
    }
    
    fun getBindingByScriptId(scriptId: String): TileBinding? =
        bindings.values.find { it.scriptId == scriptId }
    
    fun updateToggleState(slotIndex: Int, isOn: Boolean) {
        getBinding(slotIndex)?.let { binding ->
            bindings[slotIndex] = binding.copy(toggleState = isOn)
        }
    }
    
    fun cycleQuickSelect(slotIndex: Int): String? {
        val binding = getBinding(slotIndex) ?: return null
        if (binding.quickSelectOptions.isEmpty()) return null
        
        val nextIndex = (binding.quickSelectIndex + 1) % binding.quickSelectOptions.size
        bindings[slotIndex] = binding.copy(quickSelectIndex = nextIndex)
        return binding.quickSelectOptions[nextIndex]
    }
}

