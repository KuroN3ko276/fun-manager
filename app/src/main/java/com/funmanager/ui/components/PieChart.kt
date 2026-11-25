package com.funmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class PieSlice(val value: Float, val color: Color)

/** Biểu đồ vòng (donut); [center] hiển thị ở giữa. */
@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapColor = MaterialTheme.colorScheme.surface
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val strokeWidth = size.minDimension * 0.16f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) {
                drawArc(emptyColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
                return@Canvas
            }
            var start = -90f
            slices.forEach { slice ->
                val sweep = slice.value / total * 360f
                drawArc(slice.color, start, sweep, false, topLeft, arcSize, style = Stroke(strokeWidth))
                start += sweep
            }
            if (slices.size > 1) {
                start = -90f
                slices.forEach { slice ->
                    drawArc(gapColor, start - 0.6f, 1.2f, false, topLeft, arcSize, style = Stroke(strokeWidth))
                    start += slice.value / total * 360f
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
    }
}

@Preview(showBackground = true, widthDp = 240)
@Composable
private fun PieChartPreview() {
    PieChart(
        listOf(PieSlice(5f, Color(0xFFEF5350)), PieSlice(3f, Color(0xFF42A5F5)), PieSlice(2f, Color(0xFFFFA726))),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
}

/** Khoảng cách gợi ý quanh biểu đồ. */
val PieChartPadding = 24.dp
