package com.funmanager.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.funmanager.R
import com.funmanager.data.GoalWithSaved
import com.funmanager.domain.GoalProgress
import com.funmanager.domain.formatVnd
import com.funmanager.domain.monthlyNeeded
import com.funmanager.ui.components.IconBubble
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.components.iconFor
import com.funmanager.ui.components.shortDate
import com.funmanager.ui.theme.IncomeColor
import java.time.LocalDate
import kotlin.math.roundToInt

val GoalWithSaved.progress: GoalProgress get() = GoalProgress(saved, goal.target)

/** Dòng mô tả tiến độ: đã đạt / cần để dành mỗi tháng / quá hạn. */
@Composable
fun goalStatusText(item: GoalWithSaved, today: LocalDate = LocalDate.now()): String? {
    val progress = item.progress
    val deadline = item.goal.deadline
    return when {
        progress.reached -> stringResource(R.string.goal_reached)
        deadline == null -> stringResource(R.string.goal_remaining, formatVnd(progress.remaining))
        deadline < today -> stringResource(R.string.goal_overdue, deadline.shortDate(), formatVnd(progress.remaining))
        else -> monthlyNeeded(progress, today, deadline)?.let {
            stringResource(R.string.goal_monthly_needed, formatVnd(it), deadline.shortDate())
        }
    }
}

@Composable
fun GoalCard(item: GoalWithSaved, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val color = Color(item.goal.color)
    val progress = item.progress
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(iconFor(item.goal.icon), color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.goal.name, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(item.saved, color = color)
                        Text(" / " + formatVnd(item.goal.target), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "${(progress.ratio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
            }
            LinearProgressIndicator(
                progress = { progress.ratio },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            goalStatusText(item)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.reached) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { content() }
    }
}
