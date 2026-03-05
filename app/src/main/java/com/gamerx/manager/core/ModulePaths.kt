package com.gamerx.manager.core

import com.gamerx.manager.core.ShellManager

/**
 * Auto-detects the GamerX module path by scanning installed modules.
 * Handles any module ID (gamerx_manager, gamerx_manager_module, etc.)
 */
object ModulePaths {
    
    private var cachedPath: String? = null
    
    /**
     * Get the module base path, auto-detecting from installed modules.
     * Falls back to common paths if detection fails.
     */
    suspend fun getModulePath(): String {
        cachedPath?.let { return it }
        
        // Try known paths first (fast check)
        val candidates = listOf(
            "/data/adb/modules/gamerx_manager",
            "/data/adb/modules/gamerx_manager_module",
            "/data/adb/modules/gamerx"
        )
        
        for (path in candidates) {
            val (exit, output) = ShellManager.runCommand("[ -d \"$path\" ] && echo 'yes'")
            if (exit == 0 && output.trim() == "yes") {
                cachedPath = path
                return path
            }
        }
        
        // Auto-scan: find any module with "gamerx" in module.prop
        val (exit, output) = ShellManager.runCommand(
            "grep -rl 'gamerx' /data/adb/modules/*/module.prop 2>/dev/null | head -1"
        )
        if (exit == 0 && output.isNotBlank()) {
            val propPath = output.trim()
            // Extract directory: /data/adb/modules/SOMETHING/module.prop -> /data/adb/modules/SOMETHING
            val modulePath = propPath.substringBeforeLast("/module.prop")
            if (modulePath.isNotBlank()) {
                cachedPath = modulePath
                return modulePath
            }
        }
        
        // Ultimate fallback
        cachedPath = "/data/adb/modules/gamerx_manager"
        return cachedPath!!
    }
    
    fun clearCache() {
        cachedPath = null
    }
}
