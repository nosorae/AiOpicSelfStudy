---
paths:
  - "**/src/iosMain/**"
  - "**/src/iosTest/**"
  - "apps/opic/iosApp/**"
---

# iosMain 규칙

iOS 쪽 코드는 Kotlin iosMain 에 모으고 Swift 는 최소로 둔다. Swift 는 `MainViewController()` 하나만 부르고, DI 그래프는 iosMain 에서 만든다.

- 최소 iOS 18 이다. 타깃은 `iosArm64`, `iosSimulatorArm64` 둘이다
- 녹음은 몇 초 단위 조각 파일로 바로 쓴다. 화면이 꺼지거나 다른 앱으로 가도 녹음이 이어지도록 백그라운드 오디오 모드를 쓴다
- 전화, Siri 가 끼어들면 오디오 세션 중단 알림으로 그 구간을 표시하고, 끝나면 새 조각으로 다시 녹음한다
- API 키는 Keychain 에 `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` 로 둔다
- 답변 분석 대기열은 DB 에 두고, 앱이 앞으로 올 때 순서대로 비운다
- Kotlin/Native 에서 넘어오는 Objective-C 객체는 쓰고 나서 해제되는 범위를 좁게 잡는다(`memScoped`, `autoreleasepool`)
- 시작 분기는 실행 화면(Launch Screen)이 떠 있는 동안 한다. 스플래시 화면을 따로 그리지 않는다
- DB 테스트는 실제 SQLite 드라이버로 도는 iOS 시뮬레이터 테스트(`iosSimulatorArm64Test`)에 둔다
- Xcode 빌드와 시뮬레이터는 iPhone 17 로 맞춘다
