package com.gamerx.manager.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Tools : Screen("tools_screen")
    object Customize : Screen("customize_screen")
    object Settings : Screen("settings_screen")
    
    // Feature Sub-screens
    object Performance : Screen("performance_screen")
    object Tweaks : Screen("tweaks_screen")
    object Spoofer : Screen("spoofer_screen")
    object Linux : Screen("linux_screen")
    object DeviceInfo : Screen("device_info_screen")
    object IconPack : Screen("icon_pack_screen")
    object BootAnim : Screen("boot_anim_screen")
    object Shell : Screen("shell_screen")
    object IconPackDetail : Screen("icon_pack_detail/{packName}")
    object BootAnimDetail : Screen("boot_anim_detail/{animName}")
    object SystemSounds : Screen("system_sounds_screen")
    object Scripts : Screen("script_screen")
    object ScriptEditor : Screen("script_editor/{scriptId}")
}

