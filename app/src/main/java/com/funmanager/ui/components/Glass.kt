package com.funmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.funmanager.ui.theme.LocalAppColors

/** Nền gradient có các mảng màu loang (đặt phía sau toàn bộ app). */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier.fillMaxSize().background(colors.backdrop)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Vị trí & bán kính tương đối của 3 mảng màu.
            val spots = listOf(
                Triple(Offset(w * 0.05f, h * 0.08f), w * 0.95f, 0),
                Triple(Offset(w * 1.0f, h * 0.38f), w * 0.85f, 1),
                Triple(Offset(w * 0.2f, h * 0.92f), w * 1.0f, 2),
            )
            spots.forEach { (center, radius, i) ->
                val c = colors.blobs[i % colors.blobs.size]
                drawCircle(
                    brush = Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = center, radius = radius),
                    radius = radius,
                    center = center,
                )
            }
        }
        content()
    }
}

/** Thẻ kính mờ: nền bán trong suốt, viền sáng mảnh, ánh sáng nhẹ ở mép trên. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    strong: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val container = if (strong) colors.glassStrong else colors.glass
    val cardColors = CardDefaults.cardColors(containerColor = container)
    val border = BorderStroke(
        1.dp,
        Brush.verticalGradient(listOf(colors.glassBorder, colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.3f))),
    )
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = cardColors, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = cardColors, border = border, content = content)
    }
}

/** Gradient đậm theo màu chủ đạo cho thẻ nổi bật (số dư, tổng tài sản). */
@Composable
fun accentGradient(): Brush {
    val c = LocalAppColors.current
    return Brush.linearGradient(listOf(c.primary, c.tertiary))
}

/** Màu chữ trên [accentGradient]. */
@Composable
fun onAccentGradient(): Color = MaterialTheme.colorScheme.onPrimary
