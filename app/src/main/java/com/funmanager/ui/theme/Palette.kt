package com.funmanager.ui.theme

import kotlin.math.abs
import kotlin.math.pow

/** Màu chủ đạo người dùng chọn. [seed] là màu ARGB gốc. */
enum class Accent(val label: String, val seed: Long) {
    EMERALD("Lục bảo", 0xFF2E7D32),
    OCEAN("Đại dương", 0xFF1E88E5),
    LAVENDER("Oải hương", 0xFF7E57C2),
    PEACH("Hồng đào", 0xFFD81B60),
    SUNSET("Hoàng hôn", 0xFFF4511E),
    TEAL("Ngọc lam", 0xFF00897B),
    INDIGO("Chàm", 0xFF3949AB),
    AMBER("Hổ phách", 0xFFFFB300),
    ;

    companion object {
        val DEFAULT = EMERALD
        fun fromName(name: String?): Accent = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Màu HSL: h 0..360, s 0..1, l 0..1. */
data class Hsl(val h: Float, val s: Float, val l: Float) {
    fun withL(l: Float) = copy(l = l.coerceIn(0f, 1f))
    fun withS(s: Float) = copy(s = s.coerceIn(0f, 1f))
    fun rotate(deg: Float) = copy(h = ((h + deg) % 360f + 360f) % 360f)

    /** Cùng sắc độ, chỉnh L để đạt độ sáng cảm nhận [luminance] (0..1, theo WCAG). */
    fun atLuminance(luminance: Double): Hsl {
        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2
            if (relativeLuminance(withL(mid).toArgb()) < luminance) lo = mid else hi = mid
        }
        return withL((lo + hi) / 2)
    }

    fun toArgb(alpha: Float = 1f): Long {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255).toLong()
        val a = (alpha * 255f).toInt().coerceIn(0, 255).toLong()
        return (a shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    companion object {
        fun fromArgb(argb: Long): Hsl {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2
            val d = max - min
            if (d == 0f) return Hsl(0f, 0f, l)
            val s = d / (1 - abs(2 * l - 1))
            val h = when (max) {
                r -> 60 * (((g - b) / d) % 6)
                g -> 60 * ((b - r) / d + 2)
                else -> 60 * ((r - g) / d + 4)
            }
            return Hsl((h + 360) % 360, s.coerceIn(0f, 1f), l)
        }
    }
}

/** Bộ màu (ARGB) cho một chế độ sáng/tối, sinh từ màu chủ đạo. */
data class Palette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    /** Màu nền gốc phía sau các mảng gradient. */
    val backdrop: Long,
    /** Ba mảng màu loang tạo hiệu ứng gradient phía sau lớp kính. */
    val blobs: List<Long>,
    /** Lớp kính của thẻ, thanh điều hướng, viền kính. */
    val glass: Long,
    val glassStrong: Long,
    val glassBorder: Long,
    /** Nền cho hộp thoại / menu (đục để dễ đọc). */
    val dialog: Long,
    val track: Long,
    val income: Long,
    val expense: Long,
    val warning: Long,
)

fun paletteFor(accent: Accent, dark: Boolean): Palette {
    val seed = Hsl.fromArgb(accent.seed)
    val s = seed.s.coerceIn(0.45f, 0.85f)
    val base = seed.withS(s)
    val secondary = base.rotate(-25f).withS(s * 0.55f)
    val tertiary = base.rotate(40f).withS(s * 0.8f)
    val neutral = base.withS(0.15f)
    // Mỗi vai trò màu được đặt theo độ sáng cảm nhận (luminance) nên mọi màu chủ đạo có độ tương phản như nhau.
    fun Hsl.lum(l: Double, alpha: Float = 1f) = atLuminance(l).toArgb(alpha)
    return if (!dark) {
        Palette(
            primary = base.lum(0.075),
            onPrimary = 0xFFFFFFFF,
            primaryContainer = base.lum(0.62),
            onPrimaryContainer = base.lum(0.018),
            secondary = secondary.lum(0.09),
            secondaryContainer = secondary.lum(0.66),
            onSecondaryContainer = secondary.lum(0.018),
            tertiary = tertiary.lum(0.09),
            onSurface = neutral.lum(0.012),
            onSurfaceVariant = neutral.lum(0.06),
            backdrop = base.withS(0.35f).lum(0.86),
            blobs = listOf(
                base.lum(0.45, 0.75f),
                tertiary.lum(0.52, 0.7f),
                secondary.withS(0.6f).lum(0.58, 0.7f),
            ),
            glass = 0xB3FFFFFF, // trắng 70%
            glassStrong = 0xDEFFFFFF,
            glassBorder = 0xCCFFFFFF,
            dialog = base.withS(0.25f).lum(0.90),
            track = base.withS(0.2f).lum(0.62),
            income = 0xFF1B7A2E,
            expense = 0xFFC62828,
            warning = 0xFFA35200,
        )
    } else {
        Palette(
            primary = base.lum(0.42),
            onPrimary = base.lum(0.012),
            primaryContainer = base.lum(0.035),
            onPrimaryContainer = base.lum(0.70),
            secondary = secondary.lum(0.45),
            secondaryContainer = secondary.lum(0.03),
            onSecondaryContainer = secondary.lum(0.72),
            tertiary = tertiary.lum(0.45),
            onSurface = 0xFFF1F1F4,
            onSurfaceVariant = 0xFFCFCED6,
            backdrop = base.withS(0.30f).lum(0.004),
            blobs = listOf(
                base.lum(0.035, 0.7f),
                tertiary.lum(0.03, 0.6f),
                secondary.withS(0.5f).lum(0.025, 0.6f),
            ),
            glass = 0x1FFFFFFF, // trắng 12%
            glassStrong = 0x33FFFFFF,
            glassBorder = 0x33FFFFFF,
            dialog = base.withS(0.2f).lum(0.012),
            track = 0x33FFFFFF,
            income = 0xFF8FE3A0,
            expense = 0xFFFFA4A0,
            warning = 0xFFFFC46B,
        )
    }
}

/** Độ sáng tương đối theo WCAG (bỏ qua alpha). */
fun relativeLuminance(argb: Long): Double {
    fun lin(c: Long): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * lin((argb shr 16) and 0xFF) + 0.7152 * lin((argb shr 8) and 0xFF) + 0.0722 * lin(argb and 0xFF)
}

fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** Trộn màu [top] có alpha lên nền đục [bottom]. */
fun composite(top: Long, bottom: Long): Long {
    val a = ((top shr 24) and 0xFF) / 255.0
    fun mix(shift: Int): Long {
        val t = (top shr shift) and 0xFF
        val b = (bottom shr shift) and 0xFF
        return (t * a + b * (1 - a)).toLong().coerceIn(0, 255)
    }
    return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}
