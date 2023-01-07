package com.dokar.draggablemenu.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
fun SlidingUpLayout(
    isExpanded: Boolean,
    onRequestChangeExpandedState: (Boolean) -> Unit,
    expandable: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = spring(),
    swipeGestureEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val expandProgress = remember { Animatable(0f) }

    LaunchedEffect(isExpanded) {
        expandProgress.stop()
        expandProgress.animateTo(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = animationSpec,
        )
    }

    Layout(
        content = {
            Box { expandable() }
            Box { content() }
        },
        modifier = modifier
            .pointerInput(isExpanded, swipeGestureEnabled, onRequestChangeExpandedState) {
                var totalDragAmount = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDragAmount = 0f },
                    onDragEnd = {
                        if (!swipeGestureEnabled) {
                            return@detectVerticalDragGestures
                        }
                        val threshold = 56.dp.toPx()
                        if (isExpanded && totalDragAmount > threshold) {
                            onRequestChangeExpandedState(false)
                        } else if (!isExpanded && totalDragAmount < -threshold) {
                            onRequestChangeExpandedState(true)
                        }
                    },
                    onDragCancel = {},
                    onVerticalDrag = { _, dragAmount -> totalDragAmount += dragAmount },
                )
            },
    ) { measurables, constraints ->
        val expandablePlaceable = measurables.first().measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
            )
        )
        val contentPlaceable = measurables[1].measure(constraints)

        val expandableHeight = expandablePlaceable.height

        layout(contentPlaceable.width, contentPlaceable.height) {
            val contentOffsetY = -(expandableHeight * expandProgress.value).toInt()
            contentPlaceable.place(0, contentOffsetY)

            if (expandProgress.value > 0f) {
                val expandableOffsetY = contentPlaceable.height -
                        (expandableHeight * expandProgress.value).toInt()
                expandablePlaceable.place(0, expandableOffsetY)
            }
        }
    }
}