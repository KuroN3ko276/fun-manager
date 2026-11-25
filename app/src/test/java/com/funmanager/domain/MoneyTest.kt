package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsWithDotSeparators() {
        assertEquals("0 ₫", formatVnd(0))
        assertEquals("999 ₫", formatVnd(999))
        assertEquals("1.000 ₫", formatVnd(1_000))
        assertEquals("1.250.000 ₫", formatVnd(1_250_000))
        assertEquals("-50.000 ₫", formatVnd(-50_000))
    }

    @Test
    fun parsesUserInput() {
        assertEquals(1_250_000L, parseVnd("1.250.000"))
        assertEquals(50_000L, parseVnd("50,000 ₫"))
        assertNull(parseVnd(""))
        assertNull(parseVnd("abc"))
    }

    @Test
    fun groupsDigits() {
        assertEquals("1.250.000", groupDigits("1250000"))
        assertEquals("12", groupDigits("12"))
    }
}
