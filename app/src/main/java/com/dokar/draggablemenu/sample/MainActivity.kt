package com.dokar.draggablemenu.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dokar.draggablemenu.ClippedShadowSurface
import com.dokar.draggablemenu.DraggableMenu
import com.dokar.draggablemenu.DraggableMenuDefaults
import com.dokar.draggablemenu.DraggableMenuState
import com.dokar.draggablemenu.draggableMenuAnchor
import com.dokar.draggablemenu.draggableMenuContainer
import com.dokar.draggablemenu.rememberDraggableMenuState
import com.dokar.draggablemenu.sample.theme.DraggableMenuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val defaultDarkTheme = isSystemInDarkTheme()

            var darkTheme by remember(defaultDarkTheme) { mutableStateOf(defaultDarkTheme) }

            DraggableMenuTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Sample(
                        darkTheme = darkTheme,
                        onChangeDarkTheme = { darkTheme = it },
                    )
                }
            }
        }
    }
}

@Composable
fun Sample(
    darkTheme: Boolean,
    onChangeDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val menuState = rememberDraggableMenuState()

    var isExpanded by remember { mutableStateOf(false) }

    var anchorPosition by remember { mutableStateOf(AnchorPosition.Default) }

    var optionsHeight by remember { mutableStateOf(0) }

    SlidingUpLayout(
        isExpanded = isExpanded,
        onRequestChangeExpandedState = { isExpanded = it },
        expandable = {
            SampleOptions(
                anchorPosition = anchorPosition,
                onChangeAnchorPosition = { anchorPosition = it },
                darkTheme = darkTheme,
                onChangeDarkTheme = onChangeDarkTheme,
                modifier = Modifier.onSizeChanged { optionsHeight = it.height },
            )
        },
        modifier = modifier,
        swipeGestureEnabled = !menuState.isMenuShowing,
    ) {
        SampleContent(
            isOptionsShowing = isExpanded,
            optionsHeight = optionsHeight,
            menuState = menuState,
            onExpandClick = { isExpanded = !isExpanded },
            anchorPosition = anchorPosition,
        )
    }
}

@Composable
fun SampleContent(
    isOptionsShowing: Boolean,
    optionsHeight: Int,
    menuState: DraggableMenuState,
    onExpandClick: () -> Unit,
    anchorPosition: AnchorPosition,
    modifier: Modifier = Modifier,
) {
    val menuItems = remember {
        listOf(
            SimpleMenuItem(
                icon = R.drawable.ic_twitter,
                backgroundColor = Color(0xFF1DA1F2),
                title = "Twitter",
            ),
            SimpleMenuItem(
                icon = R.drawable.ic_pinterest,
                backgroundColor = Color(0xFFE71D27),
                title = "Pinterest"
            ),
            SimpleMenuItem(
                icon = R.drawable.ic_facebook,
                backgroundColor = Color(0xFF1877F2),
                title = "Facebook"
            ),
            SimpleMenuItem(
                icon = R.drawable.ic_whatsapp,
                backgroundColor = Color(0xFF25D366),
                title = "Whatsapp"
            ),
            SimpleMenuItem(
                icon = R.drawable.ic_instagram,
                backgroundColor = Color(0xFfF70191),
                title = "Instagram"
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .gradientBackground()
            .draggableMenuContainer(state = menuState)
            .padding(32.dp),
    ) {
        val anchorAlignment: Alignment
        val anchorOffsetY: Float
        val menuAlignment: Alignment
        val menuOffset: DpOffset
        when (anchorPosition) {
            AnchorPosition.Default -> {
                anchorAlignment = Alignment.Center
                anchorOffsetY = with(LocalDensity.current) { 150.dp.toPx() }
                menuAlignment = Alignment.BottomCenter
                menuOffset = DraggableMenuDefaults.Offset
            }

            AnchorPosition.TopEnd -> {
                anchorAlignment = Alignment.TopEnd
                anchorOffsetY = with(LocalDensity.current) {
                    WindowInsets.statusBars.getTop(this).toFloat()
                }
                menuAlignment = Alignment.TopCenter
                menuOffset = DpOffset(0.dp, -DraggableMenuDefaults.Offset.y)
            }

            AnchorPosition.BottomStart -> {
                anchorAlignment = Alignment.BottomStart
                anchorOffsetY = 0f
                menuAlignment = Alignment.BottomCenter
                menuOffset = DraggableMenuDefaults.Offset
            }
        }

        fun calcFinalAnchorOffsetY(): Float {
            return if (isOptionsShowing && anchorPosition == AnchorPosition.TopEnd) {
                anchorOffsetY + optionsHeight
            } else {
                anchorOffsetY
            }
        }

        val finalAnchorOffsetY = remember(
            anchorOffsetY,
            optionsHeight,
            anchorPosition
        ) {
            Animatable(calcFinalAnchorOffsetY())
        }

        LaunchedEffect(finalAnchorOffsetY, isOptionsShowing) {
            val offsetY = calcFinalAnchorOffsetY()
            if (finalAnchorOffsetY.targetValue != offsetY) {
                finalAnchorOffsetY.animateTo(offsetY)
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, finalAnchorOffsetY.value.toInt()) }
                .align(anchorAlignment),
        ) {
            ClippedShadowSurface(
                shape = CircleShape,
                elevation = 4.dp,
                backgroundColor = DraggableMenuDefaults.backgroundColor(),
                modifier = Modifier.draggableMenuAnchor(state = menuState),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }

            DraggableMenu(
                state = menuState,
                onItemSelected = {},
                alignment = menuAlignment,
                offset = menuOffset,
            ) {
                items(menuItems) {
                    MenuItem(item = it)
                }
            }
        }

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(90f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpandClick,
                ),
            tint = Color.White,
        )
    }
}

fun Modifier.gradientBackground(): Modifier {
    return drawWithCache {
        val brush = Brush.linearGradient(
            0f to Color.Cyan,
            1f to Color.Magenta,
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
        )
        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DraggableMenuTheme {
        Sample(
            darkTheme = false,
            onChangeDarkTheme = {},
        )
    }
}