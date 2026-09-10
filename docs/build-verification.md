# Build verification

## Verified revision

- Git commit: `e46ad39e020821bbec2f83c6de61f56f45e2d0c5`
- Verified on: 2026-09-11 (Asia/Tokyo)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 34,543,091 bytes
- APK SHA-256: `2a10774ce05f43fb291ff73c56c741a4200c51ad128c52f1414b72fce654f5de`

## Local verification

Command:

```sh
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk \
GRADLE_USER_HOME=/Users/masuidrive/Develop/personal/android-kbd/tickets/260910-163036-implement-native-ime/tmp/gradle-home \
scripts/test-all.sh
```

Result: PASS.

- PDH fast checks: 5 passed
- Android unit tests: 31 passed, 0 failed, 0 errors, 0 skipped
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
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.masuidrive.gestureime.conversion.MozcConversionEngineTest
```

Result on `Medium_Phone_API_36.1(AVD)` API 36: 3 passed, 0 failed, 0 errors, 0 skipped. This suite verifies empty input, kana-to-candidate conversion and commit, and a long reading whose conversion spans multiple Mozc segments.

Raw reports:

- `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_36.1(AVD) - 16-_app-.xml`
- `app/build/reports/androidTests/connected/debug/index.html`
