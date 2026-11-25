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
