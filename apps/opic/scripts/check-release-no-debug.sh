#!/usr/bin/env bash
# 릴리스 APK 와 Release iOS 프레임워크에 실제 디버깅 도구(libs/debug)의 클래스가 0개인지 본다.
# 디버그 쪽에서 같은 이름이 나오는지도 함께 봐서, 찾는 이름이 틀려 늘 0이 나오는 일을 막는다.
# DebugCore 와 DebugActivity 는 실제 모듈에만 있다. DebugTools 는 빈 모듈에도 있어 찾지 않는다.
set -euo pipefail
cd "$(dirname "$0")/../../.."

count_apk() { unzip -p "$1" 'classes*.dex' | grep -a -c -E 'libs/debug/(DebugCore|DebugActivity);' || true; }
count_lib() { nm "$1" 2>/dev/null | grep -c -E 'kclass:io\.github\.nosorae\.aiopic\.libs\.debug\.DebugCore' || true; }

./gradlew -q :apps:opic:androidApp:assembleRelease :apps:opic:androidApp:assembleDebug
release_apk=$(count_apk apps/opic/androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk)
debug_apk=$(count_apk apps/opic/androidApp/build/outputs/apk/debug/androidApp-debug.apk)

# Xcode 가 넘기는 CONFIGURATION 으로 바꿔 끼우기를 정한다(apps/opic/shared/build.gradle.kts).
CONFIGURATION=Release ./gradlew -q :apps:opic:shared:linkReleaseFrameworkIosSimulatorArm64
CONFIGURATION=Debug ./gradlew -q :apps:opic:shared:linkDebugFrameworkIosSimulatorArm64
release_ios=$(count_lib apps/opic/shared/build/bin/iosSimulatorArm64/releaseFramework/Shared.framework/Shared)
debug_ios=$(count_lib apps/opic/shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Shared)

echo "Android release=$release_apk debug=$debug_apk, iOS release=$release_ios debug=$debug_ios"
if [ "$release_apk" -ne 0 ] || [ "$release_ios" -ne 0 ] || [ "$debug_apk" -eq 0 ] || [ "$debug_ios" -eq 0 ]; then
    echo "실패: 릴리스는 0, 디버그는 1 이상이어야 한다" >&2
    exit 1
fi
