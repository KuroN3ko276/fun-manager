package com.funmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.funmanager.domain.MonthTotals
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.IncomeColor
import java.time.YearMonth

/** Biểu đồ cột đôi thu (xanh) / chi (đỏ) theo tháng; chạm vào một tháng để chọn. */
@Composable
fun IncomeExpenseBarChart(
    totals: List<MonthTotals>,
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val max = totals.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1L
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Row(modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        totals.forEach { t ->
            val isSelected = t.month == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .background(if (isSelected) highlight else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelect(t.month) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                        val barWidth = size.width / 2 - 2.dp.toPx()
                        val radius = CornerRadius(3.dp.toPx())
                        listOf(t.income to IncomeColor, t.expense to ExpenseColor).forEachIndexed { i, (value, color) ->
                            val h = size.height * value / max
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(i * (barWidth + 4.dp.toPx()), size.height - h),
                                size = Size(barWidth, h),
                                cornerRadius = radius,
                            )
                        }
                    }
                }
                Text(
                    "T${t.month.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarChartPreview() {
    val end = YearMonth.of(2026, 11)
    IncomeExpenseBarChart(
        totals = (5 downTo 0).map { MonthTotals(end.minusMonths(it.toLong()), 15_000_000, 8_000_000L + it * 1_000_000) },
        selected = end,
        onSelect = {},
    )
}
