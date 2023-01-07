package com.dokar.draggablemenu.sample

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuItem(
    item: SimpleMenuItem,
    modifier: Modifier = Modifier,
    minWidth: Dp = 260.dp,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(min = minWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.icon != 0) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = item.title,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(item.backgroundColor.copy(alpha = 0.7f))
                    .padding(6.dp),
                tint = Color.White,
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(text = item.title, fontSize = 18.sp)
    }
}

@Immutable
data class SimpleMenuItem(
    @DrawableRes
    val icon: Int,
    val backgroundColor: Color,
    val title: String,
)
