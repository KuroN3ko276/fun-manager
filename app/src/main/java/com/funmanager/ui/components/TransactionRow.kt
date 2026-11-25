package com.funmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.Transaction
import com.funmanager.data.TransactionType
import java.time.LocalDate

data class TransactionItem(val transaction: Transaction, val category: Category)

/** Tiêu đề nhóm theo ngày kèm tổng thu − chi của ngày đó. */
@Composable
fun DayHeader(date: LocalDate, items: List<TransactionItem>) {
    val net = items.sumOf {
        if (it.transaction.type == TransactionType.INCOME) it.transaction.amount else -it.transaction.amount
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(date.dayHeader(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        MoneyText(net, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Một dòng giao dịch: biểu tượng danh mục, tên, ghi chú, số tiền. */
@Composable
fun TransactionRow(item: TransactionItem, onClick: () -> Unit, extraInfo: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(item.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.name, style = MaterialTheme.typography.bodyLarge)
                if (item.transaction.recurringId != null) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.recurring),
                        modifier = Modifier.padding(start = 4.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val secondary = listOfNotNull(
                item.transaction.note.takeIf { it.isNotBlank() },
                extraInfo,
            ).joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MoneyText(item.transaction.amount, type = item.transaction.type)
    }
}
