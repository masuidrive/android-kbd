# Build verification

## Verified revision

- Git commit: `bbe40aaf2e6583a0afc79367577a1b0a1d1024b9`
- Verified on: 2026-09-11 (Asia/Tokyo)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 34,645,297 bytes
- APK SHA-256: `87ccb6734b22b6bb940995c169fcc4c36457fd95f8c583420ae84ade0e554d0b`

## Local verification

Command:

```sh
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk \
GRADLE_USER_HOME=/Users/masuidrive/Develop/personal/android-kbd/tickets/260910-163036-implement-native-ime/tmp/gradle-home \
scripts/test-all.sh
```

Result: PASS.

- PDH fast checks: 5 passed
- Android unit tests: 32 passed, 0 failed, 0 errors, 0 skipped
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
