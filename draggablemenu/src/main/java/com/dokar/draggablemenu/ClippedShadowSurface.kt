package com.dokar.draggablemenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import kotlin.math.max

@Composable
fun ClippedShadowSurface(
    shape: Shape,
    elevation: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor,
) {
    ClippedShadowSurface(
        shape = shape,
        elevation = elevation,
        backgroundColor = backgroundColor,
        modifier = modifier,
        ambientColor = ambientColor,
        spotColor = spotColor,
        content = {},
    )
}

// Original idea: https://gist.github.com/zed-alpha/3dc931720292c1f3ff31fa6a130f52cd
@Composable
fun ClippedShadowSurface(
    shape: Shape,
    elevation: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor,
    content: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shape = shape, color = backgroundColor)
                    .drawWithCache {
                        val path = Path()
                        val outline = shape.createOutline(
                            size = size,
                            layoutDirection = layoutDirection,
                            density = this
                        )
                        path.addOutline(outline)
                        onDrawWithContent {
                            clipPath(path = path, clipOp = ClipOp.Difference) {
                                this@onDrawWithContent.drawContent()
                            }
                        }
                    }
                    .shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = ambientColor,
                        spotColor = spotColor
                    ),
            )

            content()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        if (measurables.size == 1) {
            val shadowBoxPlaceable = measurables.first().measure(constraints)
            return@Layout layout(shadowBoxPlaceable.width, shadowBoxPlaceable.height) {
                shadowBoxPlaceable.place(0, 0)
            }
        }

        val placeables = List(measurables.size - 1) {
            measurables[it + 1].measure(constraints)
        }

        var width = placeables.first().width
        var height = placeables.first().height
        for (i in 1 until placeables.size) {
            width = max(width, placeables[i].width)
            height = max(width, placeables[i].height)
        }

        val shadowBoxPlaceable = measurables.first()
            .measure(constraints.copy(maxWidth = width, maxHeight = height))

        layout(width, height) {
            shadowBoxPlaceable.place(0, 0)
            placeables.forEach { it.place(0, 0) }
        }
    }
}
