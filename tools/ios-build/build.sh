#!/bin/bash
# Build CoolWearBLEModule.m into a static library for uni-app iOS cloud packaging.
# Runs on a macOS CI runner (GitHub Actions). No code signing required for .a output.
#
# NOTE: device-only (arm64-iphoneos). Cloud packaging (custom base / App archive)
# links the iphoneos slice only. A simulator slice must NOT be merged into the
# same .a with libtool: both slices are arm64, so the linker would pull in two
# copies of every symbol (duplicate symbol _OBJC_CLASS_$_CoolWearBLEModule, ...).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PLUGIN="$ROOT/nativeplugins/CoolWear-BLE/ios"
STUBS="$ROOT/tools/ios-build/stubs"
OUT="$ROOT/tools/ios-build/out"
mkdir -p "$OUT"

SRC="$PLUGIN/CoolWearBLEModule.m"
MIN_IOS="13.0"

echo "== device (arm64-iphoneos) =="
xcrun clang -arch arm64 \
  -isysroot "$(xcrun --sdk iphoneos --show-sdk-path)" \
  -mios-version-min=$MIN_IOS \
  -fobjc-arc -fmodules -O2 \
  -I"$STUBS" -F"$PLUGIN" \
  -c "$SRC" -o "$OUT/CoolWearBLEModule.o"

echo "== archive =="
xcrun libtool -static -o "$OUT/libCoolWearBLEPlugin.a" \
  "$OUT/CoolWearBLEModule.o"

echo "== verify =="
lipo -info "$OUT/libCoolWearBLEPlugin.a"
ar -t "$OUT/libCoolWearBLEPlugin.a"
echo "OK: $OUT/libCoolWearBLEPlugin.a"
