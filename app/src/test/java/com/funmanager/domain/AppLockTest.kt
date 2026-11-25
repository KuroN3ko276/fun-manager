package com.funmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    @Test
    fun validatesPin() {
        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("123456"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("1234567"))
        assertFalse(isValidPin("12a4"))
    }

    @Test
    fun hashesWithSalt() {
        val h = hashPin("1234", "salt")
        assertEquals(64, h.length)
        assertNotEquals(h, hashPin("1234", "other"))
        assertTrue(pinMatches("1234", "salt", h))
        assertFalse(pinMatches("1235", "salt", h))
    }

    @Test
    fun locksAfterTimeout() {
        assertFalse(shouldLock(enabled = false, backgroundAt = null, now = 0, timeout = LockTimeout.IMMEDIATELY))
        assertTrue(shouldLock(true, backgroundAt = null, now = 0, timeout = LockTimeout.FIVE_MINUTES))
        assertTrue(shouldLock(true, backgroundAt = 1_000, now = 1_000, timeout = LockTimeout.IMMEDIATELY))
        assertFalse(shouldLock(true, backgroundAt = 0, now = 59_999, timeout = LockTimeout.ONE_MINUTE))
        assertTrue(shouldLock(true, backgroundAt = 0, now = 60_000, timeout = LockTimeout.ONE_MINUTE))
    }

    @Test
    fun lockoutAfterTooManyFailures() {
        assertEquals(0L, lockoutRemaining(4, lastFailedAt = 1_000, now = 1_000))
        assertEquals(30_000L, lockoutRemaining(5, lastFailedAt = 1_000, now = 1_000))
        assertEquals(10_000L, lockoutRemaining(5, lastFailedAt = 1_000, now = 21_000))
        assertEquals(0L, lockoutRemaining(7, lastFailedAt = 1_000, now = 40_000))
    }
}
