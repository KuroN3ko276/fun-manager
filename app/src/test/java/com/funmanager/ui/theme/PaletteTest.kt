package com.funmanager.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    @Test
    fun hslRoundTrip() {
        listOf(0xFF2E7D32, 0xFF1E88E5, 0xFFFFB300, 0xFF000000, 0xFFFFFFFF, 0xFF808080).forEach { argb ->
            val back = Hsl.fromArgb(argb).toArgb()
            fun ch(c: Long, s: Int) = (c shr s) and 0xFF
            listOf(16, 8, 0).forEach { s -> assertTrue(kotlin.math.abs(ch(argb, s) - ch(back, s)) <= 1) }
        }
    }

    @Test
    fun accentFromNameFallsBackToDefault() {
        assertEquals(Accent.OCEAN, Accent.fromName("OCEAN"))
        assertEquals(Accent.DEFAULT, Accent.fromName("không có"))
        assertEquals(Accent.DEFAULT, Accent.fromName(null))
    }

    /** Chữ chính trên lớp kính (đặt trên nền gốc) phải đạt chuẩn WCAG AA 4.5:1 cho mọi màu, cả sáng lẫn tối. */
    @Test
    fun textOnGlassIsReadableForEveryAccent() {
        val failures = mutableListOf<String>()
        for (accent in Accent.entries) for (dark in listOf(false, true)) {
            val p = paletteFor(accent, dark)
            // Trường hợp xấu nhất: kính nằm trên mảng màu đậm nhất.
            p.blobs.forEach { blob ->
                val behind = composite(blob, p.backdrop)
                val card = composite(p.glass, behind)
                val ratio = contrastRatio(p.onSurface, card)
                if (ratio < 4.5) failures += "$accent dark=$dark onSurface=%.2f".format(ratio)
                listOf("income" to p.income, "expense" to p.expense, "warning" to p.warning).forEach { (name, money) ->
                    val r = contrastRatio(money, card)
                    if (r < 3.0) failures += "$accent dark=$dark $name=%.2f".format(r)
                }
            }
        }
        assertTrue(failures.distinct().joinToString("; "), failures.isEmpty())
    }

    @Test
    fun buttonsAreReadableForEveryAccent() {
        val failures = mutableListOf<String>()
        for (accent in Accent.entries) for (dark in listOf(false, true)) {
            val p = paletteFor(accent, dark)
            val r1 = contrastRatio(p.onPrimary, p.primary)
            val r2 = contrastRatio(p.onPrimaryContainer, p.primaryContainer)
            val r3 = contrastRatio(p.primary, composite(p.glass, p.backdrop))
            if (r1 < 4.5) failures += "$accent dark=$dark onPrimary=%.2f".format(r1)
            if (r2 < 4.5) failures += "$accent dark=$dark onPrimaryContainer=%.2f".format(r2)
            if (r3 < 3.0) failures += "$accent dark=$dark primaryOnGlass=%.2f".format(r3)
        }
        assertTrue(failures.joinToString("; "), failures.isEmpty())
    }
}
