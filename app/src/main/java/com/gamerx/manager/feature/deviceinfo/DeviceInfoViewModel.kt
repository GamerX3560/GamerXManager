package com.gamerx.manager.feature.deviceinfo

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.launch
import java.io.File

data class DeviceSpec(val category: String, val label: String, val value: String)

class DeviceInfoViewModel : ViewModel() {
    val deviceInfoList = mutableStateOf<List<DeviceSpec>>(emptyList())
    
    init {
        loadDeviceInfo()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            val list = mutableListOf<DeviceSpec>()
            
            // Basic Info
            list.add(DeviceSpec("General", "Model", "${Build.MANUFACTURER} ${Build.MODEL}"))
            list.add(DeviceSpec("General", "Android Version", Build.VERSION.RELEASE))
            list.add(DeviceSpec("General", "SDK Level", Build.VERSION.SDK_INT.toString()))
            list.add(DeviceSpec("General", "Security Patch", Build.VERSION.SECURITY_PATCH))
            
            // Build Info
            list.add(DeviceSpec("Build", "Fingerprint", Build.FINGERPRINT))
            list.add(DeviceSpec("Build", "Bootloader", Build.BOOTLOADER))
            list.add(DeviceSpec("Build", "Radio", Build.getRadioVersion() ?: "Unknown"))
            
            // Kernel Info
            val (_, kernelVersion) = ShellManager.runCommand("uname -r", asRoot = false)
            list.add(DeviceSpec("Kernel", "Version", kernelVersion))
            
            val (_, kernelArch) = ShellManager.runCommand("uname -m", asRoot = false)
            list.add(DeviceSpec("Kernel", "Architecture", kernelArch))
            
            // Memory (RAM)
            // Parse /proc/meminfo
            val (_, memInfo) = ShellManager.runCommand("cat /proc/meminfo | grep MemTotal", asRoot = false)
            list.add(DeviceSpec("Memory", "Total RAM", memInfo.replace("MemTotal:", "").trim()))
            
            deviceInfoList.value = list
        }
    }
}
