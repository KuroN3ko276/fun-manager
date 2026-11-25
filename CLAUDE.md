# Fun Manager

App Android quản lý thu chi theo tháng (Kotlin + Jetpack Compose + Room). Dữ liệu chỉ lưu trên máy.

## Lệnh

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew test            # unit test
./gradlew assembleDebug   # APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Quy tắc (đọc trước khi code)

- [Kiến trúc](.claude/rules/architecture.md)
- [Coding style](.claude/rules/coding-style.md)
- [Kiểm thử](.claude/rules/testing.md)
- [Git](.claude/rules/git.md)

## Backlog

Tính năng dự kiến: [docs/FEATURE_IDEAS.md](docs/FEATURE_IDEAS.md)
