package com.funmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funmanager.data.Category

/** Bộ biểu tượng cho danh mục; khoá được lưu trong DB. */
val CATEGORY_ICONS: List<Pair<String, ImageVector>> = listOf(
    "food" to Icons.Filled.Restaurant,
    "cafe" to Icons.Filled.LocalCafe,
    "transport" to Icons.Filled.DirectionsCar,
    "fuel" to Icons.Filled.LocalGasStation,
    "travel" to Icons.Filled.Flight,
    "home" to Icons.Filled.Home,
    "bill" to Icons.Filled.Receipt,
    "electric" to Icons.Filled.Bolt,
    "water" to Icons.Filled.WaterDrop,
    "internet" to Icons.Filled.Wifi,
    "phone" to Icons.Filled.PhoneAndroid,
    "shopping" to Icons.Filled.ShoppingCart,
    "clothes" to Icons.Filled.Checkroom,
    "entertainment" to Icons.Filled.Movie,
    "game" to Icons.Filled.SportsEsports,
    "sport" to Icons.Filled.FitnessCenter,
    "health" to Icons.Filled.LocalHospital,
    "beauty" to Icons.Filled.Spa,
    "education" to Icons.Filled.School,
    "kids" to Icons.Filled.ChildCare,
    "pets" to Icons.Filled.Pets,
    "gift" to Icons.Filled.CardGiftcard,
    "salary" to Icons.Filled.Payments,
    "work" to Icons.Filled.Work,
    "bonus" to Icons.Filled.CardGiftcard,
    "invest" to Icons.AutoMirrored.Filled.TrendingUp,
    "savings" to Icons.Filled.Savings,
    "other" to Icons.Filled.Category,
)

private val ICON_MAP = CATEGORY_ICONS.toMap()

/** Bảng màu cho danh mục (ARGB). */
val CATEGORY_COLORS: List<Long> = listOf(
    0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2, 0xFF5C6BC0, 0xFF42A5F5,
    0xFF26C6DA, 0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFFFCA28, 0xFFFFA726,
    0xFFFF7043, 0xFF8D6E63, 0xFF78909C,
)

fun iconFor(key: String): ImageVector = ICON_MAP[key] ?: Icons.Filled.Category

val Category.uiColor: Color get() = Color(color)

@Composable
fun CategoryIcon(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    IconBubble(iconFor(category.icon), category.uiColor, modifier, size)
}

@Composable
fun IconBubble(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconTint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.55f))
    }
}
