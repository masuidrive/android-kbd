#!/bin/zsh
set -euo pipefail

# Regenerates the committed arm64-v8a Mozc artifacts.  Run from the repository root.
MOZC_SRC="${MOZC_SRC:-/tmp/android-kbd-mozc/src}"
BAZELISK="${BAZELISK:-bazelisk}"
OUTPUT_ROOT="${MOZC_OUTPUT_ROOT:-/tmp/android-kbd-bazel-output}"
SCRIPT_DIR="${0:A:h}"
ROOT="${SCRIPT_DIR:h}"

if [[ ! -f "$MOZC_SRC/MODULE.bazel" ]]; then
  print -u2 "Set MOZC_SRC to a checkout of google/mozc at d918f30063999d4ca250f76ced8b92169a0e0b25."
  exit 1
fi

cd "$MOZC_SRC"
"$BAZELISK" --output_user_root="$OUTPUT_ROOT" build //data_manager/oss:mozc.data --config release_build --jobs=6
"$BAZELISK" --output_user_root="$OUTPUT_ROOT" build //android/jni:mozc.arm64 --config oss_android --config release_build --jobs=6
"$BAZELISK" --output_user_root="$OUTPUT_ROOT" build //protocol:commands_java_proto_lite --jobs=6

mkdir -p "$ROOT/app/src/main/assets" "$ROOT/app/src/main/jniLibs/arm64-v8a" "$ROOT/app/libs"
cp bazel-bin/data_manager/oss/mozc.data "$ROOT/app/src/main/assets/mozc.data"
cp bazel-out/arm64-v8a-opt-ST-5713e14ae68c/bin/android/jni/libmozc.so "$ROOT/app/src/main/jniLibs/arm64-v8a/libmozc.so"
print "Merge the five protocol lib*-lite.jar files into app/libs/mozc-proto-lite.jar, then update docs/mozc-artifacts.md hashes."
