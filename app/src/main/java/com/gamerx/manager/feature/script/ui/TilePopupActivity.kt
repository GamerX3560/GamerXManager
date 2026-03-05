package com.gamerx.manager.feature.script.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.manager.feature.script.data.ScriptRepository
import com.gamerx.manager.feature.script.data.ScriptStateManager
import com.gamerx.manager.feature.script.logic.ScriptExecutor
import com.gamerx.manager.feature.script.model.ScriptInput
import com.gamerx.manager.feature.script.model.ScriptManifest
import com.gamerx.manager.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Activity launched when a QS Tile with "popup" mode is tapped.
 * Shows script inputs and allows execution with selected values.
 */
class TilePopupActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scriptId = intent.getStringExtra("script_id") ?: run {
            finish()
            return
        }
        
        setContent {
            TilePopupScreen(
                scriptId = scriptId,
                onDismiss = { finish() },
                onExecute = { finish() }
            )
        }
    }
}

@Composable
fun TilePopupScreen(
    scriptId: String,
    onDismiss: () -> Unit,
    onExecute: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manifest by remember { mutableStateOf<ScriptManifest?>(null) }
    var inputValues by remember { mutableStateOf<MutableMap<String, Any>>(mutableMapOf()) }
    var isExecuting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(scriptId) {
        manifest = ScriptRepository.getScript(scriptId)
        manifest?.let { m ->
            // Load saved state or defaults
            val saved = ScriptStateManager.loadState(
                context,
                scriptId
            )
            val initial = mutableMapOf<String, Any>()
            m.ui.inputs.forEach { input ->
                initial[input.id] = saved[input.id] ?: getDefaultValue(input)
            }
            inputValues = initial
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = manifest?.identity?.name ?: "Script",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Inputs
                if (manifest != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        manifest!!.ui.inputs.forEach { input ->
                            PopupInput(
                                input = input,
                                value = inputValues[input.id],
                                allValues = inputValues,
                                onValueChange = { 
                                    inputValues = inputValues.toMutableMap().apply { 
                                        put(input.id, it) 
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Result message
                resultMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.contains("✅")) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Execute Button
                Button(
                    onClick = {
                        scope.launch {
                            isExecuting = true
                            resultMessage = executeScript(manifest!!, inputValues)
                            isExecuting = false
                            kotlinx.coroutines.delay(1500)
                            onExecute()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExecuting && manifest != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeManager.accentColor.value
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Execute", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopupInput(
    input: ScriptInput,
    value: Any?,
    allValues: Map<String, Any>,
    onValueChange: (Any) -> Unit
) {
    // Check dependency
    if (input.dependency != null) {
        val targetValue = allValues[input.dependency!!.target]?.toString() ?: ""
        val matches = when (input.dependency!!.condition) {
            "equals" -> targetValue == input.dependency!!.value
            "not_equals" -> targetValue != input.dependency!!.value
            else -> true
        }
        if (!matches) return
    }
    
    Column {
        Text(
            text = input.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        input.description?.let {
            Text(
                text = it,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        when (input) {
            is ScriptInput.TextInput -> {
                OutlinedTextField(
                    value = value?.toString() ?: "",
                    onValueChange = { onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.accentColor.value,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
            
            is ScriptInput.NumberInput -> {
                var textValue by remember { mutableStateOf(value?.toString() ?: input.default.toInt().toString()) }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newVal ->
                        // Only allow digits
                        val filtered = newVal.filter { it.isDigit() }
                        textValue = filtered
                        filtered.toIntOrNull()?.let { onValueChange(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.accentColor.value,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
            
            is ScriptInput.SelectInput -> {
                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = input.options.find { it.value == value?.toString() }?.label ?: "Select..."
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeManager.accentColor.value,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    ) {
                        input.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, color = Color.White) },
                                onClick = {
                                    onValueChange(option.value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            is ScriptInput.ToggleInput -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(input.label, color = Color.White)
                    Switch(
                        checked = value as? Boolean ?: false,
                        onCheckedChange = { onValueChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemeManager.accentColor.value,
                            checkedTrackColor = ThemeManager.accentColor.value.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            else -> {
                Text("Unsupported input type", color = Color.Gray)
            }
        }
    }
}

private fun getDefaultValue(input: ScriptInput): Any {
    return when (input) {
        is ScriptInput.TextInput -> input.default
        is ScriptInput.NumberInput -> input.default.toInt()
        is ScriptInput.ToggleInput -> input.default
        is ScriptInput.SelectInput -> input.default ?: input.options.firstOrNull()?.value ?: ""
        is ScriptInput.SliderInput -> input.default
        else -> ""
    }
}

private suspend fun executeScript(
    manifest: ScriptManifest,
    inputValues: Map<String, Any>
): String = withContext(Dispatchers.IO) {
    try {
        val scriptFile = ScriptRepository.getScriptFile(manifest.id, manifest.execution.entryPoint)
            ?: return@withContext "❌ Script file not found"
        
        val jsonInputs = buildJsonObject {
            inputValues.forEach { (key, value) ->
                put(key, JsonPrimitive(value.toString()))
            }
        }
        
        val result = ScriptExecutor.executeScript(manifest, scriptFile, jsonInputs)
        
        if (result.exitCode == 0) {
            "✅ ${result.stdout.take(100)}"
        } else {
            "❌ ${result.stderr.take(100)}"
        }
    } catch (e: Exception) {
        "❌ Error: ${e.message}"
    }
}
