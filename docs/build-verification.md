# Build verification

## Verified revision

- Git commit: `6ec20cc`
- Verified on: 2026-09-11 (Asia/Tokyo)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 34,685,797 bytes
- APK SHA-256: `4e691b4003dcdc883de55dd812b27fd7fba3061b408867d561ac63b821fa2cea`

## Local verification

Command:

```sh
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk \
GRADLE_USER_HOME=/Users/masuidrive/Develop/personal/android-kbd/tickets/260910-163036-implement-native-ime/tmp/gradle-home \
scripts/test-all.sh
```

Result: PASS.

- PDH fast checks: 5 passed
- Android unit tests: 62 passed, 0 failed, 0 errors, 0 skipped
- Android lint: PASS
- Debug APK assembly: PASS
- Gradle: `BUILD SUCCESSFUL`

Raw reports:

- `app/build/test-results/testDebugUnitTest/`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`

## Connected verification

Command:

```sh
./gradlew connectedDebugAndroidTest
```

Result on `Medium_Phone_API_36.1(AVD)` API 36: 7 passed, 0 failed, 0 errors, 0 skipped. Five tests verify Mozc input and conversion. Two voice probes verify that an on-device recognizer exists and report the installed-language state; they do not prove successful speech recognition. This AVD reported no installed Japanese model.

Raw reports:

- `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_36.1(AVD) - 16-_app-.xml`
- `app/build/reports/androidTests/connected/debug/index.html`
