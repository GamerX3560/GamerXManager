package com.gamerx.manager.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class AppTheme {
    OLED,      // Pure Black, White accents, No Gradients
    COLORFUL,  // Current Gradient Style
    GLASS,      // Translucent, Blur, Mesh Gradients
    RETRO_WAVE, // Synthwave style (Purple/Cyan/Grid)
    HIGH_CONTRAST // Redesigned: Cyber-Contrast (Deep Black / Neon)
}

enum class NavStyle {
    FLOATING,  // Current Floating Island
    PILL,      // Small Pill at bottom
    CLASSIC    // Full width bar
}

enum class SpecialEffect {
    NONE,
    SNOW,
    LEAVES,
    RAIN
}

object ThemeManager {
    // State
    val currentTheme = mutableStateOf(AppTheme.COLORFUL)
    val navStyle = mutableStateOf(NavStyle.FLOATING)
    val accentColor = mutableStateOf(GamerXRed)
    val customBackgroundUri = mutableStateOf<Any?>(null)
    
    // Customization State
    val uiLayerOpacity = mutableStateOf(0.5f) // 0f (Transparent) to 1f (Opaque)
    val wallpaperDim = mutableStateOf(0.0f)   // 0f (No Dim) to 1f (Black)
    val blurIntensity = mutableStateOf(10f)      // 0 to 100
    val specialEffect = mutableStateOf(SpecialEffect.NONE)
    
    // Advanced Effect Parameters
    val effectSpeed = mutableStateOf(1.0f)
    val effectSize = mutableStateOf(1.0f)
    val effectDensity = mutableStateOf(1.0f)

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "key_theme"
    private const val KEY_NAV_STYLE = "key_nav_style"
    private const val KEY_ACCENT_COLOR = "key_accent_color"
    private const val KEY_CUSTOM_BG = "key_custom_bg"
    private const val KEY_CUSTOM_BG_TYPE = "key_custom_bg_type"
    private const val KEY_UI_OPACITY = "key_ui_opacity" // Renamed from bg_opacity
    private const val KEY_WALLPAPER_DIM = "key_wallpaper_dim"
    private const val KEY_BLUR_INTENSITY = "key_blur_intensity"
    private const val KEY_SPECIAL_EFFECT = "key_special_effect"
    private const val KEY_EFFECT_SPEED = "key_effect_speed"
    private const val KEY_EFFECT_SIZE = "key_effect_size"
    private const val KEY_EFFECT_DENSITY = "key_effect_density"

    lateinit var preferences: android.content.SharedPreferences

    fun init(context: android.content.Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        loadTheme()
    }

    private fun loadTheme() {
        // Load Theme
        val themeName = preferences.getString(KEY_THEME, AppTheme.COLORFUL.name)
        currentTheme.value = try { AppTheme.valueOf(themeName!!) } catch (e: Exception) { AppTheme.COLORFUL }

        // Load Nav Style
        val navStyleName = preferences.getString(KEY_NAV_STYLE, NavStyle.FLOATING.name)
        navStyle.value = try { NavStyle.valueOf(navStyleName!!) } catch (e: Exception) { NavStyle.FLOATING }

        // Load Accent Color
        val colorInt = preferences.getInt(KEY_ACCENT_COLOR, GamerXRed.toArgb())
        accentColor.value = Color(colorInt)

        // Load Background
        val bgType = preferences.getString(KEY_CUSTOM_BG_TYPE, null)
        val bgVal = preferences.getString(KEY_CUSTOM_BG, null)
        
        if (bgType == "int" && bgVal != null) {
            customBackgroundUri.value = bgVal.toIntOrNull()
        } else if (bgType == "uri" && bgVal != null) {
            customBackgroundUri.value = bgVal // Keep as String (Uri representation)
        } else {
            customBackgroundUri.value = null
        }
        
        // Load Advanced Customization
        uiLayerOpacity.value = preferences.getFloat(KEY_UI_OPACITY, 0.5f)
        wallpaperDim.value = preferences.getFloat(KEY_WALLPAPER_DIM, 0.0f)
        blurIntensity.value = preferences.getFloat(KEY_BLUR_INTENSITY, 10f)
        val effectName = preferences.getString(KEY_SPECIAL_EFFECT, SpecialEffect.NONE.name)
        specialEffect.value = try { SpecialEffect.valueOf(effectName!!) } catch (e: Exception) { SpecialEffect.NONE }
        
        // Load Effect Parameters
        effectSpeed.value = preferences.getFloat(KEY_EFFECT_SPEED, 1.0f)
        effectSize.value = preferences.getFloat(KEY_EFFECT_SIZE, 1.0f)
        effectDensity.value = preferences.getFloat(KEY_EFFECT_DENSITY, 1.0f)
    }

    // State Converters
    fun saveTheme(theme: AppTheme) {
        currentTheme.value = theme
        preferences.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun saveNavStyle(style: NavStyle) {
        navStyle.value = style
        preferences.edit().putString(KEY_NAV_STYLE, style.name).apply()
    }

    fun saveAccentColor(color: Color) {
        accentColor.value = color
        preferences.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }
    
    fun saveUiOpacity(opacity: Float) {
        uiLayerOpacity.value = opacity
        preferences.edit().putFloat(KEY_UI_OPACITY, opacity).apply()
    }
    
    fun saveWallpaperDim(dim: Float) {
        wallpaperDim.value = dim
        preferences.edit().putFloat(KEY_WALLPAPER_DIM, dim).apply()
    }
    
    fun saveBlur(blur: Float) {
        blurIntensity.value = blur
        preferences.edit().putFloat(KEY_BLUR_INTENSITY, blur).apply()
    }
    
    fun saveSpecialEffect(effect: SpecialEffect) {
        specialEffect.value = effect
        preferences.edit().putString(KEY_SPECIAL_EFFECT, effect.name).apply()
    }
    
    fun saveEffectSpeed(speed: Float) {
        effectSpeed.value = speed
        preferences.edit().putFloat(KEY_EFFECT_SPEED, speed).apply()
    }

    fun saveEffectSize(size: Float) {
        effectSize.value = size
        preferences.edit().putFloat(KEY_EFFECT_SIZE, size).apply()
    }

    fun saveEffectDensity(density: Float) {
        effectDensity.value = density
        preferences.edit().putFloat(KEY_EFFECT_DENSITY, density).apply()
    }

    fun saveBackground(bg: Any?) {
        customBackgroundUri.value = bg
        val editor = preferences.edit()
        if (bg == null) {
            editor.remove(KEY_CUSTOM_BG).remove(KEY_CUSTOM_BG_TYPE)
        } else if (bg is Int) {
            editor.putString(KEY_CUSTOM_BG_TYPE, "int")
            editor.putString(KEY_CUSTOM_BG, bg.toString())
        } else if (bg is String) {
            editor.putString(KEY_CUSTOM_BG_TYPE, "uri")
            editor.putString(KEY_CUSTOM_BG, bg)
        }
        editor.apply()
    }

    // Logic to get background brush based on theme
    fun getBackgroundBrush(): Brush {
        return when (currentTheme.value) {
            AppTheme.OLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
            AppTheme.COLORFUL -> Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color.Black))
            AppTheme.GLASS -> Brush.verticalGradient(listOf(Color(0xFF2E003E), Color(0xFF000000))) // Dark Purple/Black base for Glass
            AppTheme.RETRO_WAVE -> Brush.verticalGradient(listOf(Color(0xFF240046), Color(0xFF10002B))) // Deep Purple
            AppTheme.HIGH_CONTRAST -> Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF000000))) // Dark Tech
        }
    }
    
    // Helper to get preset list
    val Presets = listOf(
        com.gamerx.manager.R.drawable.bg_preset_1,
        com.gamerx.manager.R.drawable.bg_preset_2,
        com.gamerx.manager.R.drawable.bg_preset_3,
        com.gamerx.manager.R.drawable.bg_preset_4
    )
    
    fun setBackground(bg: Any?) {
        saveBackground(bg)
    }

    fun getCardColor(): Color {
        val baseColor = when (currentTheme.value) {
            AppTheme.OLED -> Color(0xFF121212)
            AppTheme.COLORFUL -> Color(0xFF1E1E1E)
            AppTheme.GLASS -> Color(0xFFFFFFFF)
            AppTheme.RETRO_WAVE -> Color(0xFF3C096C)
            AppTheme.HIGH_CONTRAST -> Color(0xFF000000) // Deep Black
        }
        
        // Use the user-defined background opacity
        val opacity = uiLayerOpacity.value
        
        return when(currentTheme.value) {
            AppTheme.GLASS -> baseColor.copy(alpha = 0.1f) // Glass always needs low alpha for effect
            AppTheme.HIGH_CONTRAST -> baseColor.copy(alpha = 1f) // High Contrast always opaque
            else -> baseColor.copy(alpha = opacity)
        }
    }
    
    // Deprecated helpers maintained for compatibility but redirected to save*
    fun setDateTheme(theme: AppTheme) {
        saveTheme(theme)
    }
    
    fun setNavStyleCheck(style: NavStyle) {
        saveNavStyle(style)
    }
}

// Predefined Palettes
val Palettes = listOf(
    Color(0xFFFF1744) to "Crimson",
    Color(0xFF2962FF) to "Royal Blue",
    Color(0xFF006400) to "Dark Green",
    Color(0xFFFFD600) to "Gold",
    Color(0xFFAA00FF) to "Purple",
    Color(0xFF00E5FF) to "Cyan",
    Color(0xFFFF6D00) to "Orange",
    Color(0xFF00C853) to "Emerald",
    Color(0xFF00BFA5) to "Teal",
    Color(0xFF808080) to "Grey"
)
