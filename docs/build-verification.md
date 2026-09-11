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

## v0.4 verification

Revision `7fec052b04372f25bb9d107628e3442cce504496` was verified on 2026-09-11 JST. `scripts/test-all.sh` passed 5 fast checks, 85 unit tests, Android lint, and debug APK assembly. The raw console output is preserved at `docs/verification/v0.4-test-all.log`; the JUnit XML files under `app/build/test-results/testDebugUnitTest/` report 85 tests with no failures, errors, or skips.

The production APK is `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `8f6604d5bc433727a6bf421923c3f7557cfdb6b52d0119d94ee3b62c94246457`. It is version code 4 / version name 0.4.0 and is signed with the Android debug certificate whose SHA-256 digest is `b56a8bf1af87f450f392e11785a58a8e70ec2814c656ea47c25d7e36faf0c015`.

The connected suite ran against this final production revision and reports 7 tests with no failures, errors, or skips. Its console output is preserved at `docs/verification/v0.4-connected.log`.
