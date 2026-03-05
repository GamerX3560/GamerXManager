package com.gamerx.manager.feature.script.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * QS Tile Icon Library
 * 40+ icons organized by category for tile customization
 */
object TileIcons {
    
    data class TileIcon(
        val name: String,
        val icon: ImageVector,
        val category: String
    )
    
    // All available icons
    val allIcons: List<TileIcon> = listOf(
        // System
        TileIcon("battery", Icons.Default.BatteryFull, "System"),
        TileIcon("battery_charging", Icons.Default.BatteryChargingFull, "System"),
        TileIcon("wifi", Icons.Default.Wifi, "System"),
        TileIcon("bluetooth", Icons.Default.Bluetooth, "System"),
        TileIcon("volume", Icons.Default.VolumeUp, "System"),
        TileIcon("volume_off", Icons.Default.VolumeOff, "System"),
        TileIcon("brightness", Icons.Default.Brightness6, "System"),
        TileIcon("airplane", Icons.Default.AirplanemodeActive, "System"),
        TileIcon("do_not_disturb", Icons.Default.DoNotDisturb, "System"),
        TileIcon("flashlight", Icons.Default.FlashlightOn, "System"),
        TileIcon("location", Icons.Default.LocationOn, "System"),
        TileIcon("nfc", Icons.Default.Nfc, "System"),
        TileIcon("sync", Icons.Default.Sync, "System"),
        
        // Device
        TileIcon("memory", Icons.Default.Memory, "Device"),
        TileIcon("storage", Icons.Default.Storage, "Device"),
        TileIcon("thermostat", Icons.Default.Thermostat, "Device"),
        TileIcon("phone", Icons.Default.PhoneAndroid, "Device"),
        TileIcon("screen_rotation", Icons.Default.ScreenRotation, "Device"),
        TileIcon("dark_mode", Icons.Default.DarkMode, "Device"),
        TileIcon("developer", Icons.Default.DeveloperMode, "Device"),
        
        // Gaming
        TileIcon("gamepad", Icons.Default.SportsEsports, "Gaming"),
        TileIcon("speed", Icons.Default.Speed, "Gaming"),
        TileIcon("timer", Icons.Default.Timer, "Gaming"),
        TileIcon("trending_up", Icons.Default.TrendingUp, "Gaming"),
        TileIcon("network_check", Icons.Default.NetworkCheck, "Gaming"),
        
        // Media
        TileIcon("play", Icons.Default.PlayArrow, "Media"),
        TileIcon("pause", Icons.Default.Pause, "Media"),
        TileIcon("music", Icons.Default.MusicNote, "Media"),
        TileIcon("video", Icons.Default.Videocam, "Media"),
        TileIcon("camera", Icons.Default.CameraAlt, "Media"),
        TileIcon("mic", Icons.Default.Mic, "Media"),
        TileIcon("headphones", Icons.Default.Headphones, "Media"),
        
        // Tools
        TileIcon("terminal", Icons.Default.Terminal, "Tools"),
        TileIcon("code", Icons.Default.Code, "Tools"),
        TileIcon("settings", Icons.Default.Settings, "Tools"),
        TileIcon("build", Icons.Default.Build, "Tools"),
        TileIcon("power", Icons.Default.PowerSettingsNew, "Tools"),
        TileIcon("schedule", Icons.Default.Schedule, "Tools"),
        TileIcon("refresh", Icons.Default.Refresh, "Tools"),
        TileIcon("download", Icons.Default.Download, "Tools"),
        TileIcon("upload", Icons.Default.Upload, "Tools"),
        
        // Custom/Fun
        TileIcon("star", Icons.Default.Star, "Custom"),
        TileIcon("favorite", Icons.Default.Favorite, "Custom"),
        TileIcon("bolt", Icons.Default.Bolt, "Custom"),
        TileIcon("shield", Icons.Default.Shield, "Custom"),
        TileIcon("rocket", Icons.Default.RocketLaunch, "Custom"),
        TileIcon("auto_fix", Icons.Default.AutoFixHigh, "Custom"),
        TileIcon("diamond", Icons.Default.Diamond, "Custom"),
        TileIcon("palette", Icons.Default.Palette, "Custom"),
        TileIcon("extension", Icons.Default.Extension, "Custom"),
        TileIcon("verified", Icons.Default.Verified, "Custom")
    )
    
    // Get icon by name
    fun getIcon(name: String): ImageVector {
        return allIcons.find { it.name == name }?.icon ?: Icons.Default.PlayArrow
    }
    
    // Get icons by category
    fun getIconsByCategory(category: String): List<TileIcon> {
        return allIcons.filter { it.category == category }
    }
    
    // Get all categories
    val categories: List<String> = allIcons.map { it.category }.distinct()
}
