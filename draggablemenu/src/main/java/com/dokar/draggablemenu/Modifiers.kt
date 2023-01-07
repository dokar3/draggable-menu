package com.dokar.draggablemenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.draggableMenuContainer(state: DraggableMenuState): Modifier {
    return onGloballyPositioned { state.containerCoordinates = it }
        .pointerInput(state) {
            coroutineScope {
                awaitPointerEventScope {
                    while (isActive) {
                        awaitFirstDown()
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            if (!state.isMenuShowing) {
                                continue
                            }
                            if (!change.position.isSpecifiedAndValid()) {
                                continue
                            }
                            val offset = state.containerCoordinates?.positionInWindow()
                                ?.let { if (it.isSpecifiedAndValid()) it else Offset.Zero }
                                ?: Offset.Zero
                            val position = change.position + offset
                            state.updatePointerPosition(position)
                            if (state.isOutOfMenuBounds(position) ||
                                event.changes.all { it.changedToUp() }
                            ) {
                                state.hideMenu()
                            }
                        }
                    }
                }
            }
        }
}

fun Modifier.draggableMenuAnchor(
    state: DraggableMenuState,
    onClick: (() -> Unit)? = { state.showMenu() },
): Modifier = composed {
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }

    val interactionSource = remember { MutableInteractionSource() }

    val indication = rememberScaleIndication()

    fun shouldHideAnchor(
        isStretchingDown: Boolean,
        isStretchingUp: Boolean,
    ): Boolean {
        val menuCoordinates = state.menuCoordinates ?: return false
        val anchorCoordinates = state.anchorCoordinates ?: return false

        val menuPos = state.menuPosOnScreen
        if (!menuPos.isSpecifiedAndValid()) return false

        val anchorPos = anchorCoordinates.positionInWindow()

        val isShowAboveAnchor = menuPos.y + menuCoordinates.size.height <= anchorPos.y
        if (isShowAboveAnchor && isStretchingDown && calcStretchDownFactor(state) > 0.1f) {
            return true
        }

        val isShowBelowAnchor = menuPos.y >= anchorPos.y + anchorCoordinates.size.height
        if (isShowBelowAnchor && isStretchingUp && calcStretchUpFactor(state) > 0.1f) {
            return true
        }

        return false
    }

    LaunchedEffect(state) {
        snapshotFlow { Triple(state.isStretchingDown, state.isStretchingUp, state.hoveredItem) }
            .filter { it.first || it.second || it.third.isNone() }
            .map { shouldHideAnchor(it.first, it.second) }
            .distinctUntilChanged()
            .collect { shouldHideAnchor ->
                launch {
                    scale.stop()
                    scale.animateTo(
                        targetValue = if (shouldHideAnchor) 0f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
    }

    onGloballyPositioned { state.anchorCoordinates = it }
        .indication(interactionSource, indication)
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin.Center
        }
        .pointerInput(state) {
            coroutineScope {
                awaitPointerEventScope {
                    while (isActive) {
                        val down = awaitFirstDown()
                        val press = PressInteraction.Press(down.position)
                        scope.launch { interactionSource.emit(press) }
                        try {
                            withTimeout(timeMillis = viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            state.showMenu()
                        }
                        scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                        onClick?.invoke()
                    }
                }
            }
        }
}

internal fun Modifier.handleMenuTapAndDragGestures(state: DraggableMenuState): Modifier {
    fun onPointerPositionChanged(position: Offset): Boolean {
        val menuPos = state.menuPosOnScreen
        if (menuPos.isSpecifiedAndValid()) {
            val posOnScreen = position + menuPos
            if (state.isOutOfMenuBounds(posOnScreen)) {
                state.hideMenu()
                return true
            } else {
                state.updatePointerPosition(posOnScreen)
            }
        }
        return false
    }

    return pointerInput(state) {
        coroutineScope {
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown()

                    val downItem = state.calcHoveredItemUsingPosInMenu(down.position)
                    val press = PressInteraction.Press(down.position)
                    val interactionSource = state.getItemInteractionSource(downItem.index)
                    if (interactionSource != null) {
                        launch { interactionSource.emit(press) }
                    }

                    var isTap = false
                    var change: PointerInputChange? = null

                    try {
                        change = withTimeout(
                            timeMillis = viewConfiguration.longPressTimeoutMillis
                        ) {
                            waitForUpOrCancellation()
                        }
                        // Tap
                        val pos = change?.position ?: down.position
                        val item = state.calcHoveredItemUsingPosInMenu(pos)
                        if (!item.isNone()) {
                            state.hideMenu(hoveredItemIndex = item.index)
                        }
                        isTap = true
                    } catch (_: PointerEventTimeoutCancellationException) {
                        // Long pressed
                        val pos = change?.position ?: down.position
                        onPointerPositionChanged(pos)
                    }

                    if (interactionSource != null) {
                        launch { interactionSource.emit(PressInteraction.Release(press)) }
                    }

                    if (isTap) {
                        return@awaitPointerEventScope
                    }

                    while (isActive) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)

                        if (event.changes.all { it.changedToUp() }) {
                            // Up
                            state.hideMenu()
                            break
                        }

                        if (onPointerPositionChanged(event.changes.first().position)) {
                            break
                        }
                    }
                }
            }
        }
    }
}

internal fun Modifier.animateMenuEnterExit(
    menuState: DraggableMenuState,
    visibleState: MutableTransitionState<Boolean>,
): Modifier = composed {
    val transition = updateTransition(
        transitionState = visibleState,
        label = "DraggableMenuTransition",
    )

    val transitionValue by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy - 0.05f,
                    stiffness = Spring.StiffnessMediumLow - 50f,
                )
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            }
        },
        label = "transitionValue",
    ) { visible ->
        if (visible) 1f else 0f
    }

    graphicsLayer {
        val menuCoordinates = menuState.menuCoordinates
        val menuPos = menuState.menuPosOnScreen
        val anchorCoordinates = menuState.anchorCoordinates

        if (menuCoordinates == null ||
            !menuCoordinates.isAttached ||
            menuPos.isUnspecified ||
            anchorCoordinates == null ||
            !anchorCoordinates.isAttached
        ) {
            return@graphicsLayer
        }

        val menuPosition = menuPos + menuCoordinates.positionInWindow()
        val menuSize = menuCoordinates.size
        if (menuSize.width == 0 || menuSize.height == 0) {
            return@graphicsLayer
        }

        val anchorPosition = anchorCoordinates.positionInWindow()
        val anchorSize = anchorCoordinates.size
        val anchorCenter = Offset(
            x = anchorPosition.x + anchorSize.width / 2,
            y = anchorPosition.y + anchorSize.height / 2
        )

        val pivotX = (anchorCenter.x - menuPosition.x) / menuSize.width
        val pivotY = (anchorCenter.y - menuPosition.y) / menuSize.height
        transformOrigin = TransformOrigin(
            pivotX.coerceIn(0f, 1f),
            pivotY.coerceIn(0f, 1f)
        )
        scaleX = transitionValue
        scaleY = transitionValue

        alpha = transitionValue.coerceIn(0f, 1f)
    }
}

internal fun Modifier.stretchEffect(state: DraggableMenuState): Modifier {
    return graphicsLayer {
        if (!state.isHovered) {
            return@graphicsLayer
        }
        val hoveredItem = state.hoveredItem
        if (hoveredItem.isNone()) {
            return@graphicsLayer
        }
        if (state.isStretchingUp) {
            transformOrigin = TransformOrigin(0.5f, 1f)
            val stretch = calcStretchUpFactor(state)
            scaleX = 1f - 0.05f * stretch
            scaleY = 1f + 0.05f * stretch
        } else if (state.isStretchingDown) {
            transformOrigin = TransformOrigin(0.5f, 0f)
            val stretch = calcStretchDownFactor(state)
            scaleX = 1f - 0.05f * stretch
            scaleY = 1f + 0.05f * stretch
        }
    }
}

private fun calcStretchUpFactor(state: DraggableMenuState): Float {
    val hoveredItem = state.hoveredItem
    if (hoveredItem.pointerYToTop >= 0f) {
        return 0f
    }
    return (-hoveredItem.pointerYToTop / state.menuSize.height).coerceAtMost(1f)
}

private fun calcStretchDownFactor(state: DraggableMenuState): Float {
    val hoveredItem = state.hoveredItem
    val pointerYToBottom = hoveredItem.pointerYToTop - hoveredItem.height
    if (pointerYToBottom <= 0f) {
        return 0f
    }
    return (pointerYToBottom / state.menuSize.height).coerceAtMost(1f)
}