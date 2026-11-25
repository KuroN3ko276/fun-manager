# Kiểm thử

- Logic trong `domain/` phải có unit test JUnit (`app/src/test/...`): tổng hợp theo tháng/danh mục, % ngân sách, trạng thái cảnh báo, định dạng tiền.
- Viết test trước hoặc cùng lúc với logic (TDD khi hợp lý).
- Trước mỗi commit chạy:
  ```bash
  ./gradlew test
  ./gradlew assembleDebug
  ```
  Cả hai phải thành công. Không commit khi test đỏ.
- Không đánh dấu tính năng "xong" khi chưa build được APK.
- Đo hiệu năng (khởi động, giật lag) trên **bản release** (`installRelease`), không trên bản debug.
  Dùng `adb shell am start -W` (TotalTime) và `adb shell dumpsys gfxinfo com.funmanager` (Janky frames, percentiles).
- Sau khi đổi cấu hình R8/release, mở thử mọi màn trên máy thật để chắc R8 không làm hỏng gì.
