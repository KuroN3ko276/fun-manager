# Fun Manager

App Android quản lý thu chi theo tháng (Kotlin + Jetpack Compose + Room). Dữ liệu chỉ lưu trên máy.

## Lệnh

```bash
export JAVA_HOME=$HOME/Android/jdk-17   # JDK 17 (cần javac)
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew test            # unit test
./gradlew assembleDebug   # APK debug: app/build/outputs/apk/debug/app-debug.apk
./gradlew installRelease  # bản dùng hằng ngày (R8, nhanh hơn nhiều; ký bằng khoá debug nên cài đè được lên bản debug)
```

Bản debug khởi động chậm (~1 s, có khung hình 500 ms) — đừng đánh giá hiệu năng trên bản debug.
Bản release không debuggable nên `adb shell run-as` không đọc được DB; cần đọc DB thì cài bản debug.

```bash
```

## Quy tắc (đọc trước khi code)

- [Kiến trúc](.claude/rules/architecture.md)
- [Coding style](.claude/rules/coding-style.md)
- [Kiểm thử](.claude/rules/testing.md)
- [Git](.claude/rules/git.md)

## Backlog

Tính năng dự kiến: [docs/FEATURE_IDEAS.md](docs/FEATURE_IDEAS.md)
