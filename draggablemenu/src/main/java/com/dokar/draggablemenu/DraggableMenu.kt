package com.dokar.draggablemenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun DraggableMenu(
    state: DraggableMenuState,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter,
    offset: DpOffset = DraggableMenuDefaults.Offset,
    shape: Shape = MaterialTheme.shapes.large,
    hoverBarShape: Shape = MaterialTheme.shapes.large,
    backgroundColor: Color = DraggableMenuDefaults.backgroundColor(),
    hoverBarBackgroundColor: Color = DraggableMenuDefaults.hoverBarBackground(),
    elevation: Dp = DraggableMenuDefaults.Elevation,
    properties: PopupProperties = PopupProperties(),
    itemIndication: Indication? = rememberScaleIndication(pressedScale = 0.9f),
    content: DraggableMenuScope.() -> Unit,
) {
    val density = LocalDensity.current

    val hapticFeedback = LocalHapticFeedback.current

    val visibleState = remember { MutableTransitionState(false) }

    val popupOffset = with(density) {
        IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    }

    val contentPadding = PaddingValues(
        horizontal = 24.dp,
        vertical = 16.dp,
    )

    SideEffect {
        state.menuContentPadding = contentPadding
        state.onItemSelected = onItemSelected
    }

    LaunchedEffect(state, visibleState, hapticFeedback) {
        launch {
            snapshotFlow { state.isMenuShowing }
                .distinctUntilChanged()
                .collect { isMenuShowing ->
                    visibleState.targetState = isMenuShowing
                    if (isMenuShowing) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
        }
        launch {
            snapshotFlow { state.hoveredItem }
                .filter { it.index >= 0 }
                .map { it.index }
                .distinctUntilChanged()
                .collect {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
        }
    }

    if (visibleState.currentState || visibleState.targetState || !visibleState.isIdle) {
        Popup(
            alignment = alignment,
            offset = popupOffset,
            onDismissRequest = { state.hideMenu() },
            properties = properties,
        ) {
            val view = LocalView.current

            DraggableMenuContent(
                state = state,
                modifier = modifier
                    .onGloballyPositioned {
                        state.menuCoordinates = it
                        state.menuSize = it.size
                        val viewPos = intArrayOf(0, 0)
                        view.getLocationOnScreen(viewPos)
                        state.menuPosOnScreen = IntOffset(viewPos[0], viewPos[1]).toOffset()
                    }
                    .handleMenuTapAndDragGestures(state)
                    .stretchEffect(state)
                    .animateMenuEnterExit(
                        menuState = state,
                        visibleState = visibleState,
                    ),
                shape = shape,
                hoverBarShape = hoverBarShape,
                backgroundColor = backgroundColor,
                hoverBarBackgroundColor = hoverBarBackgroundColor,
                elevation = elevation,
                contentPadding = contentPadding,
                itemIndication = itemIndication,
                content = content,
            )
        }
    }
}

@Composable
private fun DraggableMenuContent(
    state: DraggableMenuState,
    shape: Shape,
    hoverBarShape: Shape,
    backgroundColor: Color,
    hoverBarBackgroundColor: Color,
    elevation: Dp,
    contentPadding: PaddingValues,
    itemIndication: Indication?,
    modifier: Modifier = Modifier,
    content: DraggableMenuScope.() -> Unit,
) {
    val density = LocalDensity.current

    val latestContent = rememberUpdatedState(content)

    val itemProvider by remember(state) {
        derivedStateOf {
            val provider = DraggableMenuItemProvider().also(latestContent.value)
            state.setItemProvider(provider)
            provider
        }
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min),
    ) {
        ClippedShadowSurface(
            shape = shape,
            elevation = elevation,
            backgroundColor = backgroundColor,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )

        val showHoverBar by remember(state) {
            derivedStateOf { !state.hoveredItem.isNone() }
        }

        val hoverBarAnimValue by animateFloatAsState(
            targetValue = if (showHoverBar) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "HoverBar",
        )

        val hoverHeight by remember {
            derivedStateOf {
                with(density) { state.hoverBarHeight.value.toDp() }
            }
        }

        val hoverBarShadowColor = Color.Black.copy(alpha = 0.5f)

        ClippedShadowSurface(
            shape = hoverBarShape,
            elevation = 12.dp,
            ambientColor = hoverBarShadowColor,
            spotColor = hoverBarShadowColor,
            backgroundColor = hoverBarBackgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(hoverHeight)
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    translationY = state.hoverBarOffset.value
                    scaleX = hoverBarAnimValue
                    scaleY = hoverBarAnimValue
                    alpha = hoverBarAnimValue.coerceIn(0f, 1f)
                },
        )

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(contentPadding)
                .clip(shape),
        ) {
            for ((index, itemContent) in itemProvider.itemsContents.withIndex()) {
                val isHovered = index == state.hoveredItem.index

                val offsetY = remember { Animatable(0f) }

                LaunchedEffect(isHovered) {
                    if (!isHovered) {
                        offsetY.animateTo(0f)
                    }
                }

                LaunchedEffect(state) {
                    snapshotFlow { state.hoveredItem }
                        .filter { it.index == index }
                        .collect {
                            if (it.height <= 0) return@collect
                            val halfHeight = it.height / 2f
                            val fraction = if (it.pointerYToTop <= halfHeight) {
                                (it.pointerYToTop - halfHeight) / halfHeight / 4f
                            } else {
                                (it.pointerYToTop - halfHeight) / halfHeight
                            }
                            offsetY.snapTo(fraction.coerceIn(-1f, 1f) * it.height / 10f)
                        }
                }

                val interactionSource = remember(state, index) {
                    MutableInteractionSource().also {
                        state.updateItemInteractionSource(index, it)
                    }
                }

                DraggableMenuItemWrapper(
                    isHovered = isHovered,
                    modifier = Modifier
                        .onGloballyPositioned { state.updateItemCoordinates(index, it) }
                        .graphicsLayer { translationY = offsetY.value }
                        .indication(
                            interactionSource = interactionSource,
                            indication = itemIndication,
                        ),
                    content = { itemContent() },
                )
            }
        }
    }
}

@Composable
private fun DraggableMenuItemWrapper(
    isHovered: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scale = animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = if (isHovered) {
            spring()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "Scale",
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin.Center
        },
        content = content,
    )
}
