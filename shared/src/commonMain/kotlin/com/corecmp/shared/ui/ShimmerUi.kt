package com.corecmp.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ShimmerDefaults {
    val baseColor = Color(0xFFE2E8F0)
    val highlightColor = Color(0xFFCBD5E1)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmer(
                shimmerColor = ShimmerDefaults.highlightColor,
                bgColor = ShimmerDefaults.baseColor,
            ),
    )
}

@Composable
fun ShimmerCircle(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(
        modifier = modifier.size(size),
        shape = CircleShape,
    )
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 14.dp,
) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
    )
}
