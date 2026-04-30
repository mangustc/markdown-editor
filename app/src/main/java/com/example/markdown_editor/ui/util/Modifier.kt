package com.example.markdown_editor.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
): Modifier = composed {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 150))
        } else {
            delay(1000)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
        }
    }

    drawWithContent {
        drawContent()

        val currentAlpha = alpha.value
        if (currentAlpha == 0f) return@drawWithContent

        val layoutInfo = state.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo
        if (total == 0 || visible.isEmpty()) return@drawWithContent

        val viewportHeight = layoutInfo.viewportSize.height.toFloat()
        val itemHeight = visible.map { it.size }.average().toFloat()
        if (itemHeight == 0f) return@drawWithContent

        val totalHeight = total * itemHeight
        if (totalHeight <= viewportHeight) return@drawWithContent

        val barHeight = (viewportHeight / totalHeight) * viewportHeight
        val maxScroll = totalHeight - viewportHeight
        val currentScroll =
            (state.firstVisibleItemIndex * itemHeight) + state.firstVisibleItemScrollOffset
        val fraction = (currentScroll / maxScroll).coerceIn(0f, 1f)

        val maxBarY = viewportHeight - barHeight
        val barY = if (layoutInfo.reverseLayout) {
            maxBarY - (fraction * maxBarY)
        } else {
            fraction * maxBarY
        }

        drawRoundRect(
            color = color.copy(alpha = color.alpha * currentAlpha),
            topLeft = Offset(size.width - 8.dp.toPx(), barY),
            size = Size(4.dp.toPx(), barHeight),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}