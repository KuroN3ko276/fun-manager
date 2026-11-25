package com.funmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funmanager.domain.formatVnd
import com.funmanager.ui.theme.LocalAppColors

/** Thẻ nổi bật nền gradient theo màu chủ đạo (số dư, tổng tài sản…). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = LocalAppColors.current.primary, spotColor = LocalAppColors.current.primary)
            .clip(shape)
            .background(accentGradient()),
    ) {
        // Ánh sáng loang ở góc để tạo chiều sâu.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-70).dp)
                .size(170.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/** Thẻ số dư tháng: số dư lớn + hai viên thu / chi. */
@Composable
fun BalanceHero(label: String, balance: Long, income: Long, expense: Long, incomeLabel: String, expenseLabel: String) {
    val on = onAccentGradient()
    HeroCard {
        Text(label, style = MaterialTheme.typography.titleSmall, color = on.copy(alpha = 0.85f))
        Text(
            formatVnd(balance),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = on,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroPill(Icons.AutoMirrored.Filled.TrendingUp, incomeLabel, income)
            HeroPill(Icons.AutoMirrored.Filled.TrendingDown, expenseLabel, expense)
        }
    }
}

@Composable
fun RowScope.HeroPill(icon: ImageVector, label: String, amount: Long) {
    val on = onAccentGradient()
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(on.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = on, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = on.copy(alpha = 0.85f))
            Text(
                formatVnd(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = on,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
