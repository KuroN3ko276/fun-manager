# Coding style

- Kotlin official style, 4 space indent, không dùng wildcard import.
- Tên: class `PascalCase`, hàm/biến `camelCase`, hằng `UPPER_SNAKE_CASE`.
- Tiền: luôn là `Long` đơn vị VNĐ (không dùng `Double`/`Float`). Hiển thị qua `formatVnd()` trong `domain/Money.kt` (ví dụ `1.250.000 ₫`).
- Ngày: `java.time.LocalDate`, tháng: `java.time.YearMonth`. Lưu DB dạng epoch day (`Long`) / chuỗi `yyyy-MM` qua `TypeConverter`.
- Compose:
  - State hoisting: Screen nhận `state` + lambda sự kiện; chỉ `XxxRoute` mới lấy ViewModel.
  - Mỗi Screen có ít nhất một `@Preview`.
  - Không gọi Repository trực tiếp trong composable.
- Chuỗi giao diện bằng tiếng Việt, đặt trong `res/values/strings.xml` khi có thể.
- Không để code chết, log debug hay TODO không có lý do.
