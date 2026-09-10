#!/bin/zsh
set -euo pipefail

# Regenerates every committed Mozc artifact for arm64-v8a. The source checkout
# and NDK are deliberately explicit: do not download or install toolchains here.
MOZC_REVISION="d918f30063999d4ca250f76ced8b92169a0e0b25"
MOZC_SRC="${MOZC_SRC:-/tmp/android-kbd-mozc/src}"
MOZC_NDK="${MOZC_NDK:-}"
BAZELISK="${BAZELISK:-bazelisk}"
OUTPUT_ROOT="${MOZC_OUTPUT_ROOT:-/tmp/android-kbd-bazel-output}"
JOBS="${MOZC_JOBS:-6}"
SCRIPT_DIR="${0:A:h}"
ROOT="${SCRIPT_DIR:h}"

die() {
  print -u2 -- "$*"
  exit 1
}

[[ -f "$MOZC_SRC/MODULE.bazel" ]] || die "Set MOZC_SRC to a google/mozc src checkout at $MOZC_REVISION."
[[ -n "$MOZC_NDK" && -f "$MOZC_NDK/source.properties" ]] || die "Set MOZC_NDK to an installed Android NDK (for this bundle: ndk;27.1.12297006 / r27b)."
command -v "$BAZELISK" >/dev/null || die "BAZELISK is not executable: $BAZELISK"
command -v jar >/dev/null || die "A JDK jar tool is required."

actual_revision="$(git -C "${MOZC_SRC:h}" rev-parse HEAD 2>/dev/null || true)"
[[ "$actual_revision" == "$MOZC_REVISION" ]] || die "MOZC_SRC is $actual_revision; expected $MOZC_REVISION."
ndk_revision="$(awk -F ' = ' '/^Pkg.Revision = / { print $2 }' "$MOZC_NDK/source.properties")"
[[ -n "$ndk_revision" ]] || die "Cannot read Pkg.Revision from $MOZC_NDK/source.properties."

# Mozc's Bazel extension names this directory r29. It accepts the supplied
# NDK through this path; the pinned v1 artifacts were built with r27b.
mkdir -p "$MOZC_SRC/third_party/ndk"
ln -sfn "$MOZC_NDK" "$MOZC_SRC/third_party/ndk/android-ndk-r29"

bazel=("$BAZELISK" "--output_user_root=$OUTPUT_ROOT")
run_bazel() { "${bazel[@]}" "$@"; }
artifact_for() {
  local target="$1"
  local expression="$2"
  local artifact_path
  shift 2
  artifact_path="$(run_bazel cquery "$target" "$@" --output=files | awk "$expression" | head -n 1)"
  if [[ "$artifact_path" = /* ]]; then
    print -r -- "$artifact_path"
  else
    print -r -- "$execution_root/$artifact_path"
  fi
}

cd "$MOZC_SRC"
execution_root="$(run_bazel info execution_root)"
run_bazel build //data_manager/oss:mozc.data --config=release_build --jobs="$JOBS"
run_bazel build //android/jni:mozc.arm64 --config=oss_android --config=release_build --jobs="$JOBS"
run_bazel build //protocol:commands_java_proto_lite --jobs="$JOBS"

data_path="$(artifact_for //data_manager/oss:mozc.data '/mozc\\.data$/ { print }' --config=release_build)"
native_path="$(artifact_for //android/jni:mozc.arm64 '/libmozc\\.so$/ { print }' --config=oss_android --config=release_build)"
[[ -f "$data_path" ]] || die "Bazel did not report mozc.data."
[[ -f "$native_path" ]] || die "Bazel did not report libmozc.so."

proto_targets=(candidate_window config engine_builder user_dictionary_storage commands)
proto_jars=()
for name in $proto_targets; do
  proto_path="$(artifact_for "//protocol:${name}_java_proto_lite" "/lib${name}_proto-lite\\.jar$/ { print }")"
  [[ -f "$proto_path" ]] || die "Bazel did not report ${name} protobuf lite jar."
  proto_jars+=("$proto_path")
done

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/mozc-proto.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
for proto_jar in $proto_jars; do
  (cd "$tmp_dir" && jar xf "$proto_jar")
done
rm -f "$tmp_dir/META-INF/MANIFEST.MF"

mkdir -p "$ROOT/app/src/main/assets" "$ROOT/app/src/main/jniLibs/arm64-v8a" "$ROOT/app/libs"
cp "$data_path" "$ROOT/app/src/main/assets/mozc.data"
cp "$native_path" "$ROOT/app/src/main/jniLibs/arm64-v8a/libmozc.so"
# Java 17's fixed ZIP timestamps make the merged jar byte-identical when the
# same generated inputs are used. Native and dictionary outputs can still vary
# with the toolchain and therefore remain hash-pinned in the manifest.
jar --create --date=1980-01-01T00:00:02Z --file "$ROOT/app/libs/mozc-proto-lite.jar" -C "$tmp_dir" .

print "Mozc revision: $actual_revision"
print "NDK revision: $ndk_revision"
shasum -a 256 "$ROOT/app/src/main/assets/mozc.data" "$ROOT/app/src/main/jniLibs/arm64-v8a/libmozc.so" "$ROOT/app/libs/mozc-proto-lite.jar"
print "Update docs/mozc-artifacts.md with these SHA-256 values before committing."
