#!/bin/sh
# 그레이들 없이 최소 APK 를 만들어 켜져 있는 에뮬레이터에서 돌리고 wav 를 받아 온다.
# 사용: ANDROID_HOME=<sdk> OUT_DIR=<받을 폴더> sh build_and_run.sh
set -eu
: "${ANDROID_HOME:?ANDROID_HOME 이 필요하다}"; : "${OUT_DIR:?OUT_DIR 이 필요하다}"
BT="$ANDROID_HOME/build-tools/36.0.0"; JAR="$ANDROID_HOME/platforms/android-36/android.jar"
ADB="$ANDROID_HOME/platform-tools/adb"; HERE="$(cd "$(dirname "$0")" && pwd)"
W="$(mktemp -d)"; mkdir -p "$W/assets" "$W/classes"
cp "$HERE/../questions.txt" "$W/assets/"
"$BT/aapt2" link -o "$W/base.apk" -I "$JAR" --manifest "$HERE/AndroidManifest.xml" -A "$W/assets"
javac -source 8 -target 8 -cp "$JAR" -d "$W/classes" "$HERE/src/tts/spike/Main.java" 2>/dev/null
"$BT/d8" --min-api 26 --lib "$JAR" --output "$W" $(find "$W/classes" -name '*.class')
(cd "$W" && zip -q base.apk classes.dex)
"$BT/zipalign" -f 4 "$W/base.apk" "$W/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --out "$W/tts.apk" "$W/aligned.apk"
"$ADB" install -r "$W/tts.apk"
"$ADB" logcat -c
"$ADB" shell am start -W -n tts.spike/.Main
until "$ADB" logcat -d -s TTS | grep -q "all done"; do sleep 1; done
"$ADB" logcat -d -s TTS
mkdir -p "$OUT_DIR"
for f in $("$ADB" shell ls /sdcard/Android/data/tts.spike/files/ | tr -d '\r'); do
  "$ADB" pull "/sdcard/Android/data/tts.spike/files/$f" "$OUT_DIR/"
done
