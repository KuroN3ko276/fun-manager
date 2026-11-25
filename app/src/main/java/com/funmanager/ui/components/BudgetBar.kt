package com.funmanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.funmanager.domain.BudgetLevel
import com.funmanager.domain.BudgetProgress
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.IncomeColor
import com.funmanager.ui.theme.WarningColor

@Composable
@ReadOnlyComposable
fun BudgetLevel.color(): Color = when (this) {
    BudgetLevel.OK -> IncomeColor
    BudgetLevel.WARNING -> WarningColor
    BudgetLevel.EXCEEDED -> ExpenseColor
}

@Composable
fun BudgetBar(progress: BudgetProgress, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.ratio.coerceIn(0f, 1f) },
        color = progress.level.color(),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        drawStopIndicator = {},
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        modifier = modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
    )
}
