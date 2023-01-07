package com.dokar.draggablemenu

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberDraggableMenuState(): DraggableMenuState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(scope, density, layoutDirection) {
        DraggableMenuState(scope, density, layoutDirection)
    }
}

internal data class HoveredItem(
    val index: Int,
    val height: Int = 0,
    val pointerYToTop: Float = 0f,
) {
    fun isNone() = index < 0

    companion object {
        val None = HoveredItem(index = -1)
    }
}

@Stable
class DraggableMenuState(
    private val coroutineScope: CoroutineScope,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    internal var onItemSelected: ((index: Int) -> Unit)? = null,
) {
    private var itemProvider = DraggableMenuItemProvider()

    private var itemCount: Int = 0

    var isMenuShowing by mutableStateOf(false)
        private set

    private var pointerPosition by mutableStateOf(Offset.Zero)

    private var pointerOffsetToMenuTopLeft = Offset.Zero

    internal val hoverBarHeight = Animatable(0f)
    internal val hoverBarOffset = Animatable(0f)

    internal var hoveredItem by mutableStateOf(HoveredItem.None)
        private set
    val hoveredItemIndex: Int get() = hoveredItem.index

    private var snappedItemIndex by mutableStateOf(-1)
    private var snappedOffsetY = 0f

    internal var containerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    internal var anchorCoordinates: LayoutCoordinates? by mutableStateOf(null)

    internal var menuCoordinates: LayoutCoordinates? by mutableStateOf(null)
    internal var menuSize = IntSize.Zero
    internal var menuPosOnScreen = Offset.Unspecified
    internal var menuContentPadding = PaddingValues(0.dp)

    internal var isHovered by mutableStateOf(false)
        private set

    internal var isStretchingUp by mutableStateOf(false)
        private set

    internal var isStretchingDown by mutableStateOf(false)
        private set

    private val itemCoordinatesMap = mutableMapOf<Int, LayoutCoordinates>()

    private val itemInteractionSources = mutableMapOf<Int, MutableInteractionSource>()

    internal fun setItemProvider(itemProvider: DraggableMenuItemProvider) {
        this.itemProvider = itemProvider
        this.itemCount = itemProvider.itemsContents.size
    }

    fun showMenu() {
        this.isMenuShowing = true
    }

    fun hideMenu(hoveredItemIndex: Int = hoveredItem.index) {
        isMenuShowing = false
        hoveredItem = HoveredItem.None
        snappedItemIndex = -1
        isHovered = false
        isStretchingUp = false
        isStretchingDown = false
        itemCoordinatesMap.clear()
        itemInteractionSources.clear()
        if (hoveredItemIndex != -1) {
            onItemSelected?.invoke(hoveredItemIndex)
        }
    }

    internal fun updateItemCoordinates(index: Int, coordinates: LayoutCoordinates) {
        itemCoordinatesMap[index] = coordinates
    }

    internal fun updateItemInteractionSource(index: Int, source: MutableInteractionSource) {
        itemInteractionSources[index] = source
    }

    internal fun getItemInteractionSource(index: Int): MutableInteractionSource? {
        return itemInteractionSources[index]
    }

    internal fun updatePointerPosition(positionOnScreen: Offset) {
        if (!isMenuShowing) return

        if (!positionOnScreen.isSpecifiedAndValid()) return
        pointerPosition = positionOnScreen

        if (!menuPosOnScreen.isSpecifiedAndValid()) return
        pointerOffsetToMenuTopLeft = positionOnScreen - menuPosOnScreen

        val targetItem = calcHoveredItemUsingPosInWindow(positionOnScreen)

        this.hoveredItem = targetItem

        if (!targetItem.isNone()) {
            isHovered = true
        }

        if (hoverBarHeight.targetValue.toInt() != targetItem.height) {
            coroutineScope.launch {
                hoverBarHeight.stop()
                if (hoverBarHeight.value == 0f) {
                    hoverBarHeight.snapTo(targetItem.height.toFloat())
                } else {
                    hoverBarHeight.animateTo(targetItem.height.toFloat())
                }
            }
        }

        if (snappedItemIndex != targetItem.index) {
            snappedItemIndex = targetItem.index
            coroutineScope.launch {
                animateHoverBarToSnappedItem()
                if (targetItem.index == hoveredItem.index) {
                    snappedOffsetY =
                        pointerPosition.y - menuPosOnScreen.y - hoverBarOffset.value
                }
            }
        } else if (!hoverBarOffset.isRunning) {
            coroutineScope.launch {
                moveHoverBarTo(pointerOffsetToMenuTopLeft.y - snappedOffsetY)
            }
        }

        if (isHovered) {
            isStretchingUp = targetItem.index == 0 &&
                    targetItem.pointerYToTop < 0f
            isStretchingDown = targetItem.index == itemCount - 1 &&
                    targetItem.pointerYToTop - targetItem.height > 0f
            if (isStretchingUp) {
                snappedOffsetY = 0f
            }
            if (isStretchingDown) {
                snappedOffsetY = targetItem.height.toFloat()
            }
        }
    }

    private fun calcHoveredItemUsingPosInWindow(position: Offset): HoveredItem {
        val menuOffset = this.menuPosOnScreen
        if (!menuOffset.isSpecifiedAndValid()) return HoveredItem.None

        if (position.x < menuOffset.x ||
            (position.y < menuOffset.y && !isHovered) ||
            position.x > menuOffset.x + menuSize.width
        ) {
            return HoveredItem.None
        }

        return calcHoveredItem(position - menuOffset)
    }

    internal fun calcHoveredItemUsingPosInMenu(position: Offset): HoveredItem {
        return calcHoveredItem(position)
    }

    private fun calcHoveredItem(positionToMenuTopLeft: Offset): HoveredItem {
        if (itemCount == 0) return HoveredItem.None
        try {
            val firstItemCoordinates = itemCoordinatesMap[0]
            if (firstItemCoordinates != null && isHovered) {
                val itemPos = firstItemCoordinates.positionInRoot()
                if (positionToMenuTopLeft.x >= itemPos.x &&
                    positionToMenuTopLeft.x <= itemPos.x + firstItemCoordinates.size.width &&
                    positionToMenuTopLeft.y < itemPos.y
                ) {
                    return HoveredItem(
                        index = 0,
                        height = firstItemCoordinates.size.height,
                        pointerYToTop = positionToMenuTopLeft.y,
                    )
                }
            }

            val lastItemCoordinates = itemCoordinatesMap[itemCount - 1]
            if (lastItemCoordinates != null && isHovered) {
                val itemPos = lastItemCoordinates.positionInRoot()
                if (positionToMenuTopLeft.x >= itemPos.x &&
                    positionToMenuTopLeft.x <= itemPos.x + lastItemCoordinates.size.width &&
                    positionToMenuTopLeft.y >= itemPos.y + lastItemCoordinates.size.height
                ) {
                    return HoveredItem(
                        index = itemCount - 1,
                        height = lastItemCoordinates.size.height,
                        pointerYToTop = positionToMenuTopLeft.y - itemPos.y,
                    )
                }
            }

            val menuCoordinates = this.menuCoordinates ?: return HoveredItem.None
            if (positionToMenuTopLeft.y > menuCoordinates.size.height) {
                return HoveredItem.None
            }

            for (index in 0 until itemCount) {
                val itemCoordinates = itemCoordinatesMap[index] ?: continue
                if (!itemCoordinates.isAttached) continue
                val itemPos = itemCoordinates.positionInRoot()
                if (positionToMenuTopLeft.x >= itemPos.x &&
                    positionToMenuTopLeft.y >= itemPos.y &&
                    positionToMenuTopLeft.x <= itemPos.x + itemCoordinates.size.width &&
                    positionToMenuTopLeft.y <= itemPos.y + itemCoordinates.size.height
                ) {
                    return HoveredItem(
                        index = index,
                        height = itemCoordinates.size.height,
                        pointerYToTop = positionToMenuTopLeft.y - itemPos.y,
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return HoveredItem.None
    }

    private suspend fun animateHoverBarToSnappedItem() {
        val itemCoordinates = itemCoordinatesMap[snappedItemIndex] ?: return
        try {
            val offset = itemCoordinates.positionInRoot()
            hoverBarOffset.stop()
            hoverBarOffset.animateTo(offset.y)
        } catch (_: IllegalArgumentException) {
        }
    }

    private suspend fun moveHoverBarTo(y: Float) {
        val menuContentBounds = getMenuContentBoundsInRoot() ?: return
        try {
            val targetY = y.coerceIn(
                menuContentBounds.top,
                menuContentBounds.bottom - hoverBarHeight.value,
            )
            hoverBarOffset.snapTo(targetY)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun getMenuContentBoundsInRoot(): Rect? {
        val menuCoordinates = this.menuCoordinates ?: return null
        if (!menuCoordinates.isAttached) return null
        val pos = menuCoordinates.positionInRoot()
        val size = menuCoordinates.size
        return with(density) {
            val paddingLeft = menuContentPadding.calculateLeftPadding(layoutDirection).toPx()
            val paddingTop = menuContentPadding.calculateTopPadding().toPx()
            val paddingRight = menuContentPadding.calculateRightPadding(layoutDirection).toPx()
            val paddingBottom = menuContentPadding.calculateBottomPadding().toPx()

            Rect(
                left = pos.x + paddingLeft,
                top = pos.y + paddingTop,
                right = pos.x + size.width - paddingRight,
                bottom = pos.y + size.height - paddingBottom,
            )
        }
    }

    internal fun isOutOfMenuBounds(positionOnScreen: Offset): Boolean {
        val anchorCoordinates = anchorCoordinates ?: return true
        val anchorSize = anchorCoordinates.size
        val anchorPos = anchorCoordinates.positionInWindow()
        if (positionOnScreen.x >= anchorPos.x &&
            positionOnScreen.y >= anchorPos.y &&
            positionOnScreen.x <= anchorPos.x + anchorSize.width &&
            positionOnScreen.y <= anchorPos.y + anchorSize.height
        ) {
            // Inside the anchor
            return false
        }


        if (positionOnScreen.y + menuSize.height >= anchorPos.y) {
            val v1 = Offset(
                anchorPos.x + anchorSize.width / 2,
                anchorPos.y + anchorSize.height / 2,
            )
            val v2 = Offset(
                menuPosOnScreen.x,
                menuPosOnScreen.y
            )
            val v3 = Offset(
                menuPosOnScreen.x + menuSize.width,
                menuPosOnScreen.y
            )
            if (isPointInsideTriangle(positionOnScreen, v1, v2, v3)) {
                // Inside the top triangle gap
                return false
            }
        }

        if (positionOnScreen.y <= anchorPos.y + anchorSize.height) {
            val v1 = Offset(
                anchorPos.x + anchorSize.width / 2,
                anchorPos.y + anchorSize.height / 2,
            )
            val v2 = Offset(
                menuPosOnScreen.x,
                menuPosOnScreen.y + menuSize.height
            )
            val v3 = Offset(
                menuPosOnScreen.x + menuSize.width,
                menuPosOnScreen.y + menuSize.height
            )
            if (isPointInsideTriangle(positionOnScreen, v1, v2, v3)) {
                // Inside the bottom triangle gap
                return false
            }
        }

        return positionOnScreen.x < menuPosOnScreen.x ||
                (positionOnScreen.y < menuPosOnScreen.y && !isHovered) ||
                positionOnScreen.x > menuPosOnScreen.x + menuSize.width ||
                (positionOnScreen.y > menuPosOnScreen.y + menuSize.height && !isHovered)
    }

    // Source: https://stackoverflow.com/a/2049593
    private fun isPointInsideTriangle(pt: Offset, v1: Offset, v2: Offset, v3: Offset): Boolean {
        val d1 = sign(pt, v1, v2)
        val d2 = sign(pt, v2, v3)
        val d3 = sign(pt, v3, v1)

        val hasNeg = d1 < 0 || d2 < 0 || d3 < 0
        val hasPos = d1 > 0 || d2 > 0 || d3 > 0

        return !(hasNeg && hasPos)
    }

    private fun sign(p1: Offset, p2: Offset, p3: Offset): Float {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }
}
