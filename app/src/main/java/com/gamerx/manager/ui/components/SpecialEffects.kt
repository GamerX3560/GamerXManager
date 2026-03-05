package com.gamerx.manager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gamerx.manager.ui.theme.SpecialEffect
import com.gamerx.manager.ui.theme.ThemeManager
import kotlin.random.Random

@Composable
fun SpecialEffectOverlay(effect: SpecialEffect) {
    if (effect == SpecialEffect.NONE) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Read user settings
    val userDensity = ThemeManager.effectDensity.value
    val userSpeed = ThemeManager.effectSpeed.value
    val userSize = ThemeManager.effectSize.value
    val accentColor = ThemeManager.accentColor.value

    // Create particles based on effect and user density
    val particles = remember(effect, userDensity) {
        val baseCount = when (effect) {
            SpecialEffect.SNOW -> 100
            SpecialEffect.RAIN -> 200
            SpecialEffect.LEAVES -> 50
            else -> 0
        }
        val count = (baseCount * userDensity).toInt()
        
        List(count) { 
            Particle(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                speed = Random.nextFloat() + 0.5f, // Base speed 0.5 - 1.5
                radius = Random.nextFloat(), // 0.0 - 1.0 (multiplier)
                angle = Random.nextFloat() * 360f
            ) 
        }
    }

    // Animation Loop
    val infiniteTransition = rememberInfiniteTransition()
    val frame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dt = frame * 0 + 1f // Trigger redraw
        
        particles.forEach { p ->
            updateParticle(p, effect, screenWidth, screenHeight, userSpeed)
            drawParticle(p, effect, accentColor, userSize)
        }
    }
}

// Simple mutable particle class
class Particle(
    var x: Float,
    var y: Float,
    var speed: Float, // Internal random speed factor
    var radius: Float, // Internal random size factor
    var angle: Float
)

fun updateParticle(p: Particle, effect: SpecialEffect, width: Float, height: Float, fluidSpeed: Float) {
    // fluidSpeed is the user multiplier (0.1x to 3.0x)
    
    when (effect) {
        SpecialEffect.SNOW -> {
            p.y += (1f + p.speed * 2f) * fluidSpeed
            p.x += (Math.sin(p.angle.toDouble() / 57.0).toFloat() * 0.5f) * fluidSpeed
            p.angle += 2f * fluidSpeed
            if (p.y > height) {
                p.y = -10f
                p.x = Random.nextFloat() * width
            }
        }
        SpecialEffect.RAIN -> {
            // Rain needs to be much faster
            val rainSpeed = (15f + p.speed * 20f) * fluidSpeed
            p.y += rainSpeed
            if (p.y > height) {
                p.y = -20f
                p.x = Random.nextFloat() * width
            }
        }
        SpecialEffect.LEAVES -> {
            p.y += (1f + p.speed) * fluidSpeed
            p.x += (Math.cos(p.angle.toDouble() / 57.0).toFloat() * 2.0f) * fluidSpeed
            p.angle += 1f * fluidSpeed
            if (p.y > height + 20f) {
                p.y = -20f
                p.x = Random.nextFloat() * width
            }
        }
        else -> {}
    }
}

fun DrawScope.drawParticle(p: Particle, effect: SpecialEffect, accentColor: Color, fluidSize: Float) {
    // fluidSize is user multiplier (0.5x to 2.0x)
    
    when (effect) {
        SpecialEffect.SNOW -> {
            val r = (2f + p.radius * 3f) * fluidSize
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = r,
                center = Offset(p.x, p.y)
            )
        }
        SpecialEffect.RAIN -> {
            // Fix: Make rain more visible
            val length = (20f + p.radius * 15f) * fluidSize
            val stroke = (1f + p.radius) * fluidSize
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(p.x, p.y),
                end = Offset(p.x, p.y + length),
                strokeWidth = stroke
            )
        }
        SpecialEffect.LEAVES -> {
            // Dynamic Accent Color
            val r = (4f + p.radius * 6f) * fluidSize
            drawCircle(
                color = accentColor.copy(alpha = 0.6f),
                radius = r,
                center = Offset(p.x, p.y)
            )
        }
        else -> {}
    }
}
