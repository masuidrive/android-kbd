# Bundled Mozc artifacts

Source: `google/mozc` commit `d918f30063999d4ca250f76ced8b92169a0e0b25`.
Build toolchain: Bazelisk 1.28.1 / Bazel 9.0.2, Android NDK r27b (`27.1.12297006`) for this build, and protobuf 34.1 (Android runtime `protobuf-javalite:4.34.1`).

| Artifact | SHA-256 |
| --- | --- |
| `app/src/main/assets/mozc.data` | `720d0cb42651fe35692578ea5f3c752940b8357fc24efc46230c904d2f5d5d70` |
| `app/src/main/jniLibs/arm64-v8a/libmozc.so` | `eef5a9c2077bfec4bc3f794a60f0cad5555f880bc218def8f105fd543b32da49` |
| `app/libs/mozc-proto-lite.jar` | `ce1027c4ad9ac05f632847a9f81c1519978bb6117214921fc89fe6d61c55ee9f` |

`scripts/build-mozc.sh` rebuilds the source artifacts without a hard-coded Bazel configuration directory. It requires an already-cloned source checkout, JDK, Bazelisk, and an explicitly supplied NDK. On macOS with the recorded toolchain:

```sh
git clone https://github.com/google/mozc.git /tmp/android-kbd-mozc
git -C /tmp/android-kbd-mozc checkout d918f30063999d4ca250f76ced8b92169a0e0b25
sdkmanager "ndk;27.1.12297006"
MOZC_SRC=/tmp/android-kbd-mozc/src \
MOZC_NDK="$ANDROID_SDK_ROOT/ndk/27.1.12297006" \
BAZELISK=/path/to/bazelisk \
scripts/build-mozc.sh
```

The script verifies the Mozc revision and NDK metadata, queries Bazel for `mozc.data` and `libmozc.so`, and merges the five generated protobuf lite jars into `app/libs/mozc-proto-lite.jar`. Its Java 17 `jar --date` option fixes output-entry timestamps, so the merged jar is byte-identical when its five generated inputs are identical. Native and dictionary artifacts can vary with the toolchain; their table entries are the checked-in artifact hashes, not a cross-toolchain reproducibility claim. The script prints new hashes; update the table before committing. The bundled binaries are arm64-v8a only, matching the Fold7 and the configured emulator.

The native library was checked with `llvm-objdump -p`; every `LOAD` segment is aligned to `2**14` (16 KB). The debug APK was checked with `zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk` and passed. `MozcConversionEngine` rejects the upstream minimal-engine sentinel data version `0.0.0`, so a failed dictionary load cannot be presented as a usable conversion engine.

`NOTICE-MOZC.md` and `THIRD_PARTY_NOTICES/` record the Mozc, dictionary, Abseil, Protocol Buffers, and zlib notices. Matching copies are packaged at `assets/licenses/` so APK distribution retains them.
