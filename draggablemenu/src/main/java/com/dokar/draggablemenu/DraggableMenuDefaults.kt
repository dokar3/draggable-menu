package com.dokar.draggablemenu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

object DraggableMenuDefaults {
    val Elevation = 12.dp

    val Offset = DpOffset(0.dp, -(48.dp))

    @Composable
    fun backgroundColor(): Color {
        return MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    }

    @Composable
    fun hoverBarBackground(): Color {
        val surface = MaterialTheme.colorScheme.surface
        return remember(surface) {
            Color.White.copy(alpha = 0.3f).compositeOver(surface)
        }
    }
}