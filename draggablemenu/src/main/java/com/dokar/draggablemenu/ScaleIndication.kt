package com.dokar.draggablemenu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
internal fun rememberScaleIndication(
    pressedScale: Float = 0.85f,
): ScaleIndication {
    return remember(pressedScale) { ScaleIndication(pressedScale) }
}

internal class ScaleIndication(
    private val pressedScale: Float,
) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed by interactionSource.collectIsPressedAsState()

        val scale = animateFloatAsState(targetValue = if (isPressed) pressedScale else 1f)

        return remember(scale) {
            ScaleIndicationInstance { scale.value }
        }
    }
}

private class ScaleIndicationInstance(
    private val scaleProvider: () -> Float,
) : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        val contentScope = this
        withTransform(
            transformBlock = {
                scale(scaleProvider())
            },
            drawBlock = {
                contentScope.drawContent()
            }
        )
    }
}