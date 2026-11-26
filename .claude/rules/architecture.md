# Kiến trúc

- Một module `app`, package gốc `com.funmanager`.
- MVVM: `Composable` → `ViewModel` (expose `StateFlow<UiState>`) → `Repository` → Room `Dao`.
- Package:
  - `data/` — Entity, Dao, `AppDatabase`, `Repository`, converters.
  - `domain/` — logic thuần Kotlin (tổng hợp tháng, tính ngân sách, định dạng tiền). Không phụ thuộc Android → dễ unit test.
  - `ui/<feature>/` — `XxxScreen.kt` + `XxxViewModel.kt` cho mỗi màn (home, edit, report, budget).
  - `ui/components/` — composable dùng chung (PieChart, MonthSelector, MoneyText).
  - `ui/theme/` — Material 3 theme.
  - `lock/` — khoá app (AppLockManager, vân tay qua BiometricPrompt hệ thống).
  - `widget/` — widget màn hình chính (RemoteViews, không dùng Glance).
  - `reminder/` — nhắc ghi chi tiêu buổi tối (AlarmManager không chính xác + BroadcastReceiver, không dùng WorkManager).
- DI thủ công: `AppContainer` khởi tạo trong `FunManagerApp` (Application). Không dùng Hilt/Koin khi chưa cần.
- Dữ liệu chỉ lưu local (Room). Không có network, không xin quyền INTERNET.
- Đổi schema Room: tăng `version`, thêm `AutoMigration` (kèm `AutoMigrationSpec` nếu cần seed dữ liệu),
  đọc SQL sinh ra trong `app/build/generated/ksp/.../AppDatabase_AutoMigration_*` và thử nâng cấp trên máy thật trước khi commit.
- Cài đặt nhỏ (khoá app, widget, nhắc nhở) lưu bằng SharedPreferences, không dùng DataStore.
- Không thêm thư viện mới nếu có thể làm bằng Compose/AndroidX sẵn có (ví dụ biểu đồ vẽ bằng `Canvas`).
