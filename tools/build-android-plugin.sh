#!/bin/bash
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAVAC="/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home/bin/javac"
JAR="/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home/bin/jar"
BUILD="$ROOT/tools/build-out"
PLUGIN="$ROOT/nativeplugins/CoolWear-BLE"
ANDROID_JAR="$ROOT/tools/build-deps/android.jar"
STUBS="$ROOT/tools/build-stubs"
FASTJSON="$ROOT/tools/build-deps/fastjson-1.2.83.jar"
SDK_AAR="$PLUGIN/android/libs/coolwear_bluesdk-release.aar"

if [ ! -f "$JAVAC" ]; then
  echo "未找到 JDK 8，请安装后重试"
  exit 1
fi

if [ ! -f "$ANDROID_JAR" ]; then
  echo "缺少 android.jar"
  exit 1
fi

rm -rf "$BUILD"
mkdir -p "$BUILD/sdk" "$BUILD/merge" "$BUILD/classes" "$BUILD/pack"

cd "$BUILD/sdk" && unzip -qo "$SDK_AAR"
cd "$BUILD/merge" && "$JAR" xf "$BUILD/sdk/classes.jar"

"$JAVAC" -source 1.8 -target 1.8 -encoding UTF-8 \
  -classpath "$ANDROID_JAR:$BUILD/sdk/classes.jar:$FASTJSON" \
  -d "$BUILD/classes" \
  $(find "$STUBS" -name "*.java") \
  "$PLUGIN/android/src/main/java/com/coolwear/uniplugin/CoolWearBLEModule.java"

cd "$BUILD/merge"
"$JAR" uf "$BUILD/sdk/classes.jar" -C "$BUILD/classes" com

cp "$PLUGIN/android/AndroidManifest.xml" "$BUILD/pack/AndroidManifest.xml"
cp "$BUILD/sdk/classes.jar" "$BUILD/pack/classes.jar"
if [ -f "$BUILD/sdk/R.txt" ]; then cp "$BUILD/sdk/R.txt" "$BUILD/pack/R.txt"; else printf "" > "$BUILD/pack/R.txt"; fi
if [ -d "$BUILD/sdk/res" ]; then cp -R "$BUILD/sdk/res" "$BUILD/pack/res"; fi

cd "$BUILD/pack"
"$JAR" cf "$PLUGIN/android/CoolWear-BLE.aar" .

echo "已生成: $PLUGIN/android/CoolWear-BLE.aar"
