package com.funmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Màu riêng của app ngoài ColorScheme của Material (kính, nền gradient, thu/chi). */
@Immutable
data class AppColors(
    val dark: Boolean,
    val backdrop: Color,
    val blobs: List<Color>,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val track: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val primary: Color,
    val tertiary: Color,
)

val LocalAppColors = staticCompositionLocalOf { appColors(paletteFor(Accent.DEFAULT, dark = false), dark = false) }

private fun appColors(p: Palette, dark: Boolean) = AppColors(
    dark = dark,
    backdrop = Color(p.backdrop),
    blobs = p.blobs.map(::Color),
    glass = Color(p.glass),
    glassStrong = Color(p.glassStrong),
    glassBorder = Color(p.glassBorder),
    track = Color(p.track),
    income = Color(p.income),
    expense = Color(p.expense),
    warning = Color(p.warning),
    primary = Color(p.primary),
    tertiary = Color(p.tertiary),
)

/** Màu tiền vào / tiền ra / cảnh báo theo theme hiện tại. */
val IncomeColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.income
val ExpenseColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expense
val WarningColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun colorScheme(p: Palette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(p.primary),
        onPrimary = Color(p.onPrimary),
        primaryContainer = Color(p.primaryContainer),
        onPrimaryContainer = Color(p.onPrimaryContainer),
        secondary = Color(p.secondary),
        secondaryContainer = Color(p.secondaryContainer),
        onSecondaryContainer = Color(p.onSecondaryContainer),
        tertiary = Color(p.tertiary),
        // Nền & surface trong suốt để thấy nền gradient phía sau (Scaffold, TopAppBar, Surface).
        background = Color.Transparent,
        surface = Color.Transparent,
        onBackground = Color(p.onSurface),
        onSurface = Color(p.onSurface),
        onSurfaceVariant = Color(p.onSurfaceVariant),
        surfaceVariant = Color(p.track),
        // Thẻ (Card) dùng màu kính; hộp thoại / menu / lịch dùng nền đục để dễ đọc.
        surfaceContainerHighest = Color(p.glass),
        surfaceContainerHigh = Color(p.dialog),
        surfaceContainer = Color(p.dialog),
        surfaceContainerLow = Color(p.dialog),
        surfaceContainerLowest = Color(p.dialog),
        outline = Color(p.onSurfaceVariant).copy(alpha = 0.6f),
        outlineVariant = Color(p.glassBorder),
    )
} else {
    lightColorScheme(
        primary = Color(p.primary),
        onPrimary = Color(p.onPrimary),
        primaryContainer = Color(p.primaryContainer),
        onPrimaryContainer = Color(p.onPrimaryContainer),
        secondary = Color(p.secondary),
        secondaryContainer = Color(p.secondaryContainer),
        onSecondaryContainer = Color(p.onSecondaryContainer),
        tertiary = Color(p.tertiary),
        background = Color.Transparent,
        surface = Color.Transparent,
        onBackground = Color(p.onSurface),
        onSurface = Color(p.onSurface),
        onSurfaceVariant = Color(p.onSurfaceVariant),
        surfaceVariant = Color(p.track),
        surfaceContainerHighest = Color(p.glass),
        surfaceContainerHigh = Color(p.dialog),
        surfaceContainer = Color(p.dialog),
        surfaceContainerLow = Color(p.dialog),
        surfaceContainerLowest = Color(p.dialog),
        outline = Color(p.onSurfaceVariant).copy(alpha = 0.6f),
        outlineVariant = Color(p.glassBorder),
    )
}

@Composable
fun FunManagerTheme(accent: Accent = Accent.DEFAULT, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = remember(accent, dark) { paletteFor(accent, dark) }
    val scheme = remember(palette, dark) { colorScheme(palette, dark) }
    val colors = remember(palette, dark) { appColors(palette, dark) }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, shapes = AppShapes, content = content)
    }
}
