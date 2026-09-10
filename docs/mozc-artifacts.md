# Bundled Mozc artifacts

Source: `google/mozc` commit `d918f30063999d4ca250f76ced8b92169a0e0b25`.
Build toolchain: Bazelisk 1.28.1 / Bazel 9.0.2, Android NDK r27b for this build, and protobuf 34.1 (Android runtime `protobuf-javalite:4.34.1`).

| Artifact | SHA-256 |
| --- | --- |
| `app/src/main/assets/mozc.data` | `720d0cb42651fe35692578ea5f3c752940b8357fc24efc46230c904d2f5d5d70` |
| `app/src/main/jniLibs/arm64-v8a/libmozc.so` | `eef5a9c2077bfec4bc3f794a60f0cad5555f880bc218def8f105fd543b32da49` |
| `app/libs/mozc-proto-lite.jar` | `7d6b3cb908b3b18417d7d25aa19756c25ee5f1d485639ca80b9075a9bee7f1a4` |

`scripts/build-mozc.sh` rebuilds the source artifacts. The bundled binaries are arm64-v8a only, matching the Fold7 and the configured emulator.

The native library was checked with `llvm-objdump -p`; every `LOAD` segment is aligned to `2**14` (16 KB). The debug APK was checked with `zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk` and passed. `MozcConversionEngine` rejects the upstream minimal-engine sentinel data version `0.0.0`, so a failed dictionary load cannot be presented as a usable conversion engine.
