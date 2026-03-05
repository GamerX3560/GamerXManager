package com.gamerx.manager.feature.performance

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CpuCore(val id: Int, val freq: String, val governor: String)

class PerformanceViewModel : ViewModel() {
    val cpuCores = mutableStateListOf<CpuCore>()
    
    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateCpuInfo()
                delay(2000) // Update every 2 seconds
            }
        }
    }

    private suspend fun updateCpuInfo() {
        // We generally have cpu0 to cpu7. Let's check how many cores.
        // Or simply iterate 0..7 and see what responses we get.
        val newCores = ArrayList<CpuCore>()
        
        // This is a simplified check. A robust one would check /sys/devices/system/cpu/possible
        for (i in 0..7) {
            val (exit, freq) = ShellManager.runCommand("cat /sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq", asRoot = false)
            if (exit != 0) break // Stop if we can't read (e.g., less cores)
            
            val (_, gov) = ShellManager.runCommand("cat /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor", asRoot = false)
            
            // Format freq from KHz to MHz or GHz
            val freqMhz = freq.toLongOrNull()?.div(1000) ?: 0
            
            newCores.add(CpuCore(i, "${freqMhz} MHz", gov))
        }
        
        cpuCores.clear()
        cpuCores.addAll(newCores)
    }
}
