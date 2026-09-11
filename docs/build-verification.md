# Build verification

## Verified revision

- Git commit: `7cf0f3fb189ee121914935dfd127fcdb4be6ec8c`
- Verified on: 2026-09-11 (Asia/Tokyo)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 34,654,677 bytes
- APK SHA-256: `fcd9a3cdac964d80c852d7f48abda50b97bb1ad6f3cc1b46da7f416ead08cc78`

## Local verification

Command:

```sh
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk \
GRADLE_USER_HOME=/Users/masuidrive/Develop/personal/android-kbd/tickets/260910-163036-implement-native-ime/tmp/gradle-home \
scripts/test-all.sh
```

Result: PASS.

- PDH fast checks: 5 passed
- Android unit tests: 48 passed, 0 failed, 0 errors, 0 skipped
- Android lint: PASS
- Debug APK assembly: PASS
- Gradle: `BUILD SUCCESSFUL`

Raw reports:

- `app/build/test-results/testDebugUnitTest/`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`

## Connected Mozc verification

Command:

```sh
./gradlew connectedDebugAndroidTest
```

Result on `Medium_Phone_API_36.1(AVD)` API 36: 5 passed, 0 failed, 0 errors, 0 skipped. The suite verifies empty input, kana-to-candidate conversion and commit, full-reading commits before and after Space, and fixed-input conversion latency.

Raw reports:

- `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_36.1(AVD) - 16-_app-.xml`
- `app/build/reports/androidTests/connected/debug/index.html`
