package com.funmanager.domain

import java.security.MessageDigest

const val PIN_MIN_LENGTH = 4
const val PIN_MAX_LENGTH = 6
const val MAX_FAILED_ATTEMPTS = 5
const val LOCKOUT_MS = 30_000L

/** Thời gian chờ trước khi tự khoá sau khi rời app. */
enum class LockTimeout(val millis: Long) {
    IMMEDIATELY(0),
    ONE_MINUTE(60_000),
    FIVE_MINUTES(300_000),
}

fun isValidPin(pin: String): Boolean = pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all(Char::isDigit)

/** Băm PIN kèm salt (SHA-256, dạng hex) để không lưu PIN gốc. */
fun hashPin(pin: String, salt: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/** So sánh thời gian hằng định để tránh đoán dần theo thời gian phản hồi. */
fun pinMatches(pin: String, salt: String, expectedHash: String): Boolean =
    MessageDigest.isEqual(hashPin(pin, salt).toByteArray(), expectedHash.toByteArray())

/** Có cần khoá khi quay lại app không. */
fun shouldLock(enabled: Boolean, backgroundAt: Long?, now: Long, timeout: LockTimeout): Boolean {
    if (!enabled) return false
    if (backgroundAt == null) return true
    return now - backgroundAt >= timeout.millis
}

/** Số mili giây còn phải chờ sau nhiều lần nhập sai; 0 nếu được nhập tiếp. */
fun lockoutRemaining(failedAttempts: Int, lastFailedAt: Long, now: Long): Long {
    if (failedAttempts < MAX_FAILED_ATTEMPTS) return 0
    return (lastFailedAt + LOCKOUT_MS - now).coerceAtLeast(0)
}
