package com.funmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funmanager.data.Category

fun iconFor(key: String): ImageVector = when (key) {
    "food" -> Icons.Filled.Restaurant
    "transport" -> Icons.Filled.DirectionsCar
    "home" -> Icons.Filled.Home
    "bill" -> Icons.Filled.Receipt
    "shopping" -> Icons.Filled.ShoppingCart
    "entertainment" -> Icons.Filled.Movie
    "health" -> Icons.Filled.LocalHospital
    "education" -> Icons.Filled.School
    "salary" -> Icons.Filled.Payments
    "bonus" -> Icons.Filled.CardGiftcard
    else -> Icons.Filled.Category
}

val Category.uiColor: Color get() = Color(color)

@Composable
fun CategoryIcon(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(category.uiColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconFor(category.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}
