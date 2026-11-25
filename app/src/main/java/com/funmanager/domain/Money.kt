package com.funmanager.domain

/** Định dạng số tiền VNĐ, ví dụ `1250000` → `1.250.000 ₫`. */
fun formatVnd(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return "$sign$grouped ₫"
}

/** Đọc số tiền người dùng nhập (bỏ dấu chấm, phẩy, khoảng trắng, ₫). Trả về null nếu không hợp lệ. */
fun parseVnd(input: String): Long? {
    val cleaned = input.filter { it.isDigit() }
    if (cleaned.isEmpty() || cleaned.length > 15) return null
    return cleaned.toLong()
}

/** Định dạng chuỗi đang nhập để hiển thị có dấu chấm ngăn cách, ví dụ `1250000` → `1.250.000`. */
fun groupDigits(digits: String): String =
    digits.reversed().chunked(3).joinToString(".").reversed()
