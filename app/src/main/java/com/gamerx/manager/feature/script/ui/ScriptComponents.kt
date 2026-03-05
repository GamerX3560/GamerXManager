package com.gamerx.manager.feature.script.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.manager.ui.theme.ThemeManager
import kotlin.math.roundToInt

// ==================== TEXT INPUT ====================

@Composable
fun GamerXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    description: String? = null,
    isNumber: Boolean = false,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    if (isError) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (isError) Color.Red.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(ThemeManager.accentColor.value),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ==================== TOGGLE SWITCH ====================

@Composable
fun GamerXSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeManager.accentColor.value,
                    checkedTrackColor = ThemeManager.accentColor.value.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.Transparent
                )
            )
        }
    }
}

// ==================== DROPDOWN SELECT ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamerXDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<Pair<String, String>>, // value, label
    modifier: Modifier = Modifier,
    description: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedValue }?.second ?: selectedValue
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLabel.ifEmpty { "Select..." },
                        style = TextStyle(
                            color = if (selectedValue.isEmpty()) Color.Gray else Color.White,
                            fontSize = 16.sp
                        )
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E1E1E))
            ) {
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { 
                            Text(optionLabel, color = Color.White)
                        },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        },
                        trailingIcon = if (value == selectedValue) {
                            { Icon(Icons.Default.Check, null, tint = ThemeManager.accentColor.value) }
                        } else null
                    )
                }
            }
        }
        
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ==================== SLIDER ====================

@Composable
fun GamerXSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 1f,
    unit: String = "",
    showValue: Boolean = true,
    description: String? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            if (showValue) {
                Text(
                    text = "${value.roundToInt()}$unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = ThemeManager.accentColor.value,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            steps = ((max - min) / step).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = ThemeManager.accentColor.value,
                activeTrackColor = ThemeManager.accentColor.value,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${min.roundToInt()}$unit", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("${max.roundToInt()}$unit", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ==================== MULTI-SELECT CHECKBOXES ====================

@Composable
fun GamerXMultiSelect(
    selectedValues: List<String>,
    onValuesChange: (List<String>) -> Unit,
    label: String,
    options: List<Pair<String, String>>, // value, label
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        options.forEach { (value, optionLabel) ->
            val isSelected = selectedValues.contains(value)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) ThemeManager.accentColor.value.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .clickable {
                        val newList = if (isSelected) {
                            selectedValues - value
                        } else {
                            selectedValues + value
                        }
                        onValuesChange(newList)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = ThemeManager.accentColor.value,
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = optionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
        
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ==================== INFO CARD ====================

@Composable
fun GamerXInfoCard(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    style: String = "default" // default, warning, error, success
) {
    val (bgColor, borderColor, iconColor) = when (style) {
        "warning" -> Triple(
            Color(0xFFFFA000).copy(alpha = 0.1f),
            Color(0xFFFFA000).copy(alpha = 0.3f),
            Color(0xFFFFA000)
        )
        "error" -> Triple(
            Color.Red.copy(alpha = 0.1f),
            Color.Red.copy(alpha = 0.3f),
            Color.Red
        )
        "success" -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF4CAF50).copy(alpha = 0.3f),
            Color(0xFF4CAF50)
        )
        else -> Triple(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.1f),
            ThemeManager.accentColor.value
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (style == "warning" || style == "error") Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ==================== TERMINAL CONSOLE ====================

@Composable
fun TerminalConsole(
    output: String,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Terminal Header
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                }
                Text(
                    "EXECUTION LOG",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                )
            }
            
            Divider(thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(8.dp))
            
            // Output
            Text(
                text = output.ifEmpty { "> Waiting for execution..." },
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF00E5FF),
                    lineHeight = 18.sp
                ),
                modifier = Modifier.animateContentSize()
            )
            
            // Progress
            if (isExecuting) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = ThemeManager.accentColor.value,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
