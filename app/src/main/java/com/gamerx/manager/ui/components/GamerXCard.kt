package com.gamerx.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamerx.manager.ui.theme.SurfaceVariant

@Composable
fun GamerXCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = com.gamerx.manager.ui.theme.ThemeManager.getCardColor(),
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), // Subtle glass border
        content = {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    )
}
