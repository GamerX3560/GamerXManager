package com.gamerx.manager.feature.home

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var rootStatus = mutableStateOf(false)
    var selinuxStatus = mutableStateOf("Unknown")
    var deviceModel = mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}")
    var androidVersion = mutableStateOf("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    
    // Loading state
    var isLoading = mutableStateOf(true)

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            isLoading.value = true
            rootStatus.value = ShellManager.checkRootAccess()
            
            val (_, output) = ShellManager.runCommand("getenforce")
            selinuxStatus.value = output.ifBlank { "Unknown" }
            
            isLoading.value = false
        }
    }
}
