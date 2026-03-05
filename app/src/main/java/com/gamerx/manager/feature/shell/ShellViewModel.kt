package com.gamerx.manager.feature.shell

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamerx.manager.core.ShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ShellLine(val content: String, val isInput: Boolean)

class ShellViewModel : ViewModel() {
    val outputLines = mutableStateListOf<ShellLine>()
    val commandHistory = mutableListOf<String>()
    
    fun executeCommand(command: String) {
        if (command.isBlank()) return
        
        outputLines.add(ShellLine("$ $command", true))
        commandHistory.add(command)
        
        viewModelScope.launch(Dispatchers.IO) {
            val (exit, output) = ShellManager.runCommand(command, asRoot = true)
            
            launch(Dispatchers.Main) {
                if (output.isNotBlank()) {
                    outputLines.add(ShellLine(output, false))
                } else {
                     if (exit != 0) outputLines.add(ShellLine("Command failed with exit code $exit", false))
                }
            }
        }
    }
    
    fun clear() {
        outputLines.clear()
    }
}
