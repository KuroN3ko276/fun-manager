# Fun Manager: Quản lý chi tiêu

App Android giúp ghi lại thu chi hằng ngày và xem **tiền trong tháng đã đi đâu**.
Mọi dữ liệu chỉ lưu trên điện thoại: không cần tài khoản, không cần mạng, app không xin quyền Internet.

## Tính năng

| | |
|---|---|
| **Giao dịch** | Thêm / sửa / xoá khoản chi và khoản thu, chọn danh mục, ngày, ví, ghi chú. Xem theo tháng với tổng thu, tổng chi và số dư. |
| **Báo cáo** | Biểu đồ tròn chi tiêu theo danh mục, so sánh với tháng trước, biểu đồ cột thu/chi 6 tháng gần nhất. |
| **Ngân sách** | Đặt hạn mức tổng và theo từng danh mục; hạn mức được giữ cho các tháng sau cho tới khi bạn sửa. Cảnh báo khi dùng ≥ 80% và khi vượt. |
| **Giao dịch định kỳ** | Lương, tiền nhà, tiền mạng… tự ghi vào ngày bạn chọn (1–31) mỗi tháng. Tháng ngắn hơn thì ghi vào ngày cuối tháng. |
| **Tiết kiệm** | Mục tiêu có số tiền và hạn hoàn thành. Nạp / rút có lịch sử. App tính số cần để dành mỗi tháng để kịp hạn. |
| **Nhiều ví** | Tiền mặt, ngân hàng, ví điện tử, thẻ tín dụng. Số dư từng ví, tổng tài sản, chuyển tiền giữa các ví. |
| **Tìm kiếm & lọc** | Tìm theo ghi chú / danh mục không cần gõ dấu. Lọc theo loại, danh mục, ví, khoảng ngày, khoảng tiền. |
| **Danh mục** | Thêm / sửa / xoá danh mục với 27 biểu tượng và 15 màu. |
| **Widget** | Số đã chi trong tháng và **số tiền còn lại** của tối đa 4 danh mục tự chọn. Nút ＋ để ghi nhanh. Widget không hiện thu nhập. |
| **Khoá app** | Mã PIN 4–6 số, mở bằng vân tay, tự khoá khi rời app (ngay / 1 phút / 5 phút). |

Các tính năng dự kiến làm tiếp: [docs/FEATURE_IDEAS.md](docs/FEATURE_IDEAS.md).

## Cài đặt lên điện thoại

Yêu cầu Android 8.0 (API 26) trở lên. Vân tay cần Android 11 trở lên.

**Cách 1: qua cáp USB (cần build trước, xem bên dưới)**

1. Trên điện thoại: *Cài đặt → Thông tin điện thoại → Thông tin phần mềm*, chạm 7 lần vào **Số hiệu bản tạo**.
2. Vào **Tùy chọn nhà phát triển**, bật **Gỡ lỗi USB**, cắm cáp và bấm **Cho phép** trên điện thoại.
3. Chạy:
   ```bash
   ./gradlew installRelease
   ```
   Bản **release** đã được tối ưu (R8): mở app nhanh hơn khoảng 4 lần và APK chỉ ~1,7 MB so với bản debug.

**Cách 2: chép file APK**

Sau khi build (`./gradlew assembleRelease`), gửi file `app/build/outputs/apk/release/app-release.apk` sang điện thoại rồi mở để cài.
Lần đầu cần cho phép *Cài ứng dụng không rõ nguồn gốc*.

> Cài bản mới đè lên bản cũ không làm mất dữ liệu: database được nâng cấp tự động.

## Build từ mã nguồn

### Yêu cầu

- **JDK 17** (cần `javac`, bản JRE là không đủ)
- **Android SDK**: `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`
- Không bắt buộc dùng Android Studio: có thể build hoàn toàn bằng dòng lệnh.

<details>
<summary>Cài JDK và Android SDK bằng dòng lệnh (Linux, không cần sudo)</summary>

```bash
# JDK 17 (Temurin)
mkdir -p ~/Android && cd ~/Android
curl -L -o jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
mkdir jdk-17 && tar xzf jdk.tar.gz -C jdk-17 --strip-components=1

# Android command-line tools
curl -L -o cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
mkdir -p Sdk/cmdline-tools && unzip -q cmdtools.zip -d Sdk/cmdline-tools && mv Sdk/cmdline-tools/cmdline-tools Sdk/cmdline-tools/latest

export JAVA_HOME=$HOME/Android/jdk-17
yes | Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```
</details>

### Lệnh

```bash
export JAVA_HOME=$HOME/Android/jdk-17
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew test            # chạy unit test
./gradlew assembleRelease # build APK tối ưu → app/build/outputs/apk/release/app-release.apk
./gradlew installRelease  # build và cài bản tối ưu lên điện thoại đang cắm
./gradlew installDebug    # bản debug (chậm hơn, dùng khi phát triển / cần đọc database)
```

Nếu Gradle không tìm thấy SDK, tạo file `local.properties` ở thư mục gốc (file này không được commit):

```properties
sdk.dir=/home/<user>/Android/Sdk
```

## Công nghệ

- **Kotlin 2.1** + **Jetpack Compose** (Material 3), Navigation Compose
- **Room 2.6** (SQLite) với Kotlin Flow, migration tự động giữa các phiên bản schema
- Kiến trúc **MVVM**: Composable → ViewModel (`StateFlow`) → Repository → DAO
- DI thủ công qua `AppContainer`, không dùng Hilt/Koin
- Biểu đồ vẽ bằng Compose `Canvas`. Widget dùng `RemoteViews`. Vân tay dùng `BiometricPrompt` của hệ thống. App không phụ thuộc thư viện ngoài AndroidX.
- minSdk 26, targetSdk 35, Gradle 8.11, AGP 8.7

## Cấu trúc mã nguồn

```
app/src/main/java/com/funmanager/
├── data/        Entity, DAO, AppDatabase (Room), FinanceRepository, LockStore
├── domain/      Logic thuần Kotlin: tổng hợp tháng, ngân sách, định kỳ, ví, lọc, tiết kiệm, PIN…
├── ui/
│   ├── home/      Danh sách giao dịch theo tháng
│   ├── edit/      Thêm / sửa giao dịch
│   ├── report/    Báo cáo, biểu đồ
│   ├── budget/    Ngân sách
│   ├── savings/   Mục tiêu tiết kiệm
│   ├── recurring/ Giao dịch định kỳ
│   ├── wallet/    Ví & chuyển tiền
│   ├── search/    Tìm kiếm & lọc
│   ├── category/  Quản lý danh mục
│   ├── settings/  Bảo mật (khoá app)
│   ├── lock/      Màn hình khoá, bàn phím PIN
│   ├── components/ Thành phần dùng chung (biểu đồ, chọn tháng, ô số tiền…)
│   └── theme/
├── lock/        AppLockManager, vân tay
└── widget/      Widget màn hình chính và màn cài đặt widget
app/schemas/     Schema Room đã xuất (dùng cho migration tự động)
```

## Kiểm thử

Logic nghiệp vụ nằm trong `domain/` và được kiểm tra bằng unit test JUnit (`app/src/test`):
định dạng tiền, tổng hợp tháng, ngân sách, giao dịch định kỳ, số dư ví, bộ lọc tìm kiếm, tiết kiệm, khoá PIN, widget.

```bash
./gradlew test
```

## Lưu ý về dữ liệu và bảo mật

- Dữ liệu nằm trong bộ nhớ riêng của app trên điện thoại. **Gỡ app hoặc xoá dữ liệu app sẽ làm mất toàn bộ giao dịch.**
  Tính năng xuất / sao lưu đang nằm trong danh sách dự kiến.
- Mã PIN chỉ được lưu dạng băm (SHA-256 kèm salt). **Nếu quên PIN, cách duy nhất là xoá dữ liệu app.**
- Khi bật khoá, nội dung app được ẩn khỏi màn hình đa nhiệm (Android 13+).

## Đóng góp

Quy tắc code cho người (và agent) làm việc với repo nằm trong [`.claude/rules/`](.claude/rules/):
kiến trúc, coding style, kiểm thử, git. Tóm tắt:

- Logic mới đặt trong `domain/` và có unit test.
- Chạy `./gradlew test` và `./gradlew assembleDebug` trước khi commit.
- Commit theo [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`…).
- Đổi schema Room thì tăng `version`, thêm `AutoMigration`, rồi thử nâng cấp trên máy thật.
