package com.dokar.draggablemenu.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AnchorPosition {
    Default,
    TopEnd,
    BottomStart,
}

@Composable
fun SampleOptions(
    anchorPosition: AnchorPosition,
    onChangeAnchorPosition: (AnchorPosition) -> Unit,
    darkTheme: Boolean,
    onChangeDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color(0xff333333))
            .padding(16.dp)
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (pos in AnchorPosition.values()) {
            val iconAlignment: Alignment
            val title: String
            when (pos) {
                AnchorPosition.Default -> {
                    iconAlignment = Alignment.Center
                    title = "Default"
                }

                AnchorPosition.TopEnd -> {
                    iconAlignment = Alignment.TopEnd
                    title = "Top end"
                }

                AnchorPosition.BottomStart -> {
                    iconAlignment = Alignment.BottomStart
                    title = "Bottom start"
                }
            }
            SampleOption(
                isSelected = pos == anchorPosition,
                onSelect = { onChangeAnchorPosition(pos) },
                icon = {
                    Spacer(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(12.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape,
                            ),
                    )
                },
                iconAlignment = iconAlignment,
                title = title,
            )
        }

        Spacer(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.5f))
        )

        SampleOption(
            isSelected = darkTheme,
            onSelect = { onChangeDarkTheme(!darkTheme) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_dark_mode_24),
                    contentDescription = null,
                    tint = Color.White,
                )
            },
            title = "Dark theme",
        )
    }
}

@Composable
private fun SampleOption(
    isSelected: Boolean,
    onSelect: () -> Unit,
    icon: @Composable BoxScope.() -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    iconAlignment: Alignment = Alignment.Center,
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    },
                    width = 1.dp,
                ),
            contentAlignment = iconAlignment,
        ) {
            icon()
        }

        Text(
            text = title,
            fontSize = 13.sp,
            color = Color.White,
        )
    }
}
