package com.funmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.funmanager.R
import com.funmanager.data.Category
import com.funmanager.data.TransactionType
import com.funmanager.domain.groupDigits

/** Chuẩn hoá chuỗi người dùng gõ thành số có dấu chấm ngăn cách, ví dụ `1250000` → `1.250.000`. */
fun formatAmountInput(text: String, maxDigits: Int = 13): String {
    val digits = text.filter(Char::isDigit).trimStart('0').take(maxDigits)
    return if (digits.isEmpty()) "" else groupDigits(digits)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSegmented(type: TransactionType, onChange: (TransactionType) -> Unit, modifier: Modifier = Modifier) {
    val types = listOf(TransactionType.EXPENSE to R.string.expense, TransactionType.INCOME to R.string.income)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        types.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = type == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, types.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
fun AmountField(
    text: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.amount),
) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text("₫") },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.amount_error)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.headlineSmall,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    isError: Boolean,
    title: String = stringResource(R.string.category),
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (isError) {
            Text(
                stringResource(R.string.category_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedId == category.id,
                    onClick = { onSelect(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = { CategoryIcon(category, size = 20.dp) },
                )
            }
        }
    }
}
