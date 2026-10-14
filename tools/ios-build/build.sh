#!/bin/bash
# Build CoolWearBLEModule.m into a static library for uni-app iOS cloud packaging.
# Runs on a macOS CI runner (GitHub Actions). No code signing required for .a output.
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
  -c "$SRC" -o "$OUT/CoolWearBLEModule-device.o"

echo "== simulator (arm64-iphonesimulator) =="
xcrun clang -arch arm64 \
  -isysroot "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
  -mios-version-min=$MIN_IOS \
  -fobjc-arc -fmodules -O2 \
  -I"$STUBS" -F"$PLUGIN" \
  -c "$SRC" -o "$OUT/CoolWearBLEModule-sim.o"

echo "== archive =="
xcrun libtool -static -o "$OUT/libCoolWearBLEPlugin.a" \
  "$OUT/CoolWearBLEModule-device.o" "$OUT/CoolWearBLEModule-sim.o"

echo "== verify =="
lipo -info "$OUT/libCoolWearBLEPlugin.a"
nm -g "$OUT/libCoolWearBLEPlugin.a" | grep -i CoolWearBLEModule | head -5
echo "OK: $OUT/libCoolWearBLEPlugin.a"
