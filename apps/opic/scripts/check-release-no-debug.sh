#!/usr/bin/env bash
# 릴리스 APK 와 Release iOS 프레임워크에 실제 디버깅 도구(libs/debug)의 클래스가 0개인지 본다.
# 디버그 쪽에서 같은 이름이 나오는지도 함께 봐서, 찾는 이름이 틀려 늘 0이 나오는 일을 막는다.
# DebugCore 와 DebugActivity 는 실제 모듈에만 있다. DebugTools 는 빈 모듈에도 있어 찾지 않는다.
# 가짜 응답 HTTP 엔진(ktor MockEngine)도 실제 모듈(fakeHttpEngine)을 거쳐서만 실려야 한다.
set -euo pipefail
cd "$(dirname "$0")/../../.."

count_apk() { unzip -p "$1" 'classes*.dex' | grep -a -c -E 'libs/debug/(DebugCore|DebugActivity);' || true; }
count_lib() { nm "$1" 2>/dev/null | grep -c -E 'kclass:io\.github\.nosorae\.aiopic\.libs\.debug\.DebugCore' || true; }
count_mock_apk() { unzip -p "$1" 'classes*.dex' | grep -a -c 'io/ktor/client/engine/mock/MockEngine;' || true; }
count_mock_lib() { nm "$1" 2>/dev/null | grep -c 'kclass:io\.ktor\.client\.engine\.mock\.MockEngine$' || true; }

./gradlew -q :apps:opic:androidApp:assembleRelease :apps:opic:androidApp:assembleDebug
# 서명 정보가 있으면 androidApp-release.apk, 없으면 androidApp-release-unsigned.apk 가 나온다. 앞서 만든 다른 쪽이 남아 있을 수 있어 새것을 고른다.
release_apk_file=$(ls -t apps/opic/androidApp/build/outputs/apk/release/androidApp-release*.apk | head -1)
debug_apk_file=apps/opic/androidApp/build/outputs/apk/debug/androidApp-debug.apk
release_apk=$(count_apk $release_apk_file)
debug_apk=$(count_apk $debug_apk_file)
release_apk_mock=$(count_mock_apk $release_apk_file)
debug_apk_mock=$(count_mock_apk $debug_apk_file)

# Xcode 가 넘기는 CONFIGURATION 으로 바꿔 끼우기를 정한다(apps/opic/shared/build.gradle.kts).
CONFIGURATION=Release ./gradlew -q :apps:opic:shared:linkReleaseFrameworkIosSimulatorArm64
CONFIGURATION=Debug ./gradlew -q :apps:opic:shared:linkDebugFrameworkIosSimulatorArm64
release_lib=apps/opic/shared/build/bin/iosSimulatorArm64/releaseFramework/Shared.framework/Shared
debug_lib=apps/opic/shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Shared
release_ios=$(count_lib $release_lib)
debug_ios=$(count_lib $debug_lib)
release_ios_mock=$(count_mock_lib $release_lib)
debug_ios_mock=$(count_mock_lib $debug_lib)

echo "debug module: Android release=$release_apk debug=$debug_apk, iOS release=$release_ios debug=$debug_ios"
echo "MockEngine:   Android release=$release_apk_mock debug=$debug_apk_mock, iOS release=$release_ios_mock debug=$debug_ios_mock"
if [ "$release_apk" -ne 0 ] || [ "$release_ios" -ne 0 ] || [ "$debug_apk" -eq 0 ] || [ "$debug_ios" -eq 0 ] ||
    [ "$release_apk_mock" -ne 0 ] || [ "$release_ios_mock" -ne 0 ] || [ "$debug_apk_mock" -eq 0 ] || [ "$debug_ios_mock" -eq 0 ]; then
    echo "실패: 릴리스는 0, 디버그는 1 이상이어야 한다" >&2
    exit 1
fi
