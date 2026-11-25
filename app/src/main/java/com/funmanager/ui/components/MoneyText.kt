package com.funmanager.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.funmanager.data.TransactionType
import com.funmanager.domain.formatVnd
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.IncomeColor

/** Hiển thị số tiền; nếu có [type] thì thêm dấu +/- và màu thu/chi. */
@Composable
fun MoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val (prefix, tint) = when (type) {
        TransactionType.INCOME -> "+" to IncomeColor
        TransactionType.EXPENSE -> "-" to ExpenseColor
        null -> "" to color
    }
    Text(
        text = prefix + formatVnd(amount),
        modifier = modifier,
        color = tint,
        style = style,
        fontWeight = FontWeight.SemiBold,
    )
}
