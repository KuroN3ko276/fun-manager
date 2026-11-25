# Kiến trúc

- Một module `app`, package gốc `com.funmanager`.
- MVVM: `Composable` → `ViewModel` (expose `StateFlow<UiState>`) → `Repository` → Room `Dao`.
- Package:
  - `data/` — Entity, Dao, `AppDatabase`, `Repository`, converters.
  - `domain/` — logic thuần Kotlin (tổng hợp tháng, tính ngân sách, định dạng tiền). Không phụ thuộc Android → dễ unit test.
  - `ui/<feature>/` — `XxxScreen.kt` + `XxxViewModel.kt` cho mỗi màn (home, edit, report, budget).
  - `ui/components/` — composable dùng chung (PieChart, MonthSelector, MoneyText).
  - `ui/theme/` — Material 3 theme.
- DI thủ công: `AppContainer` khởi tạo trong `FunManagerApp` (Application). Không dùng Hilt/Koin khi chưa cần.
- Dữ liệu chỉ lưu local (Room). Không có network, không xin quyền INTERNET.
- Không thêm thư viện mới nếu có thể làm bằng Compose/AndroidX sẵn có (ví dụ biểu đồ vẽ bằng `Canvas`).
