---
paths:
  - "**/src/androidMain/**"
  - "**/src/androidHostTest/**"
  - "apps/opic/androidApp/**"
---

# androidMain 규칙

Android 소스셋에는 플랫폼 API 가 꼭 필요한 구현만 둔다. 동작 규칙은 commonMain 에 있고, 여기 코드는 그 인터페이스를 구현한다.

- 최소 API 28, targetSdk 36 이다. 28 에 없는 API 는 버전 분기와 함께 쓴다
- 녹음은 몇 초 단위 조각 파일로 바로 쓴다. 화면이 꺼져도 녹음이 이어지도록 마이크 포그라운드 서비스(`foregroundServiceType="microphone"`)에서 돈다
- API 키는 Android Keystore 의 AES-GCM 키로 암호화해 DataStore 에 둔다. 평문 키를 파일, 로그, 백업에 남기지 않는다
- 답변 분석 대기열은 WorkManager 고유 작업으로 답변 ID 마다 하나씩 건다
- 녹음 폴더와 암호화된 키는 자동 백업 규칙(`dataExtractionRules`, `fullBackupContent`)에서 뺀다
- 네트워크 보안 설정으로 평문 통신을 막는다
- 릴리스 빌드는 답변 내용과 키를 로그에 남기지 않는다
- 시작 분기는 `installSplashScreen().setKeepOnScreenCondition` 으로 시스템 스플래시를 잡아 둔 채 한다. 스플래시 화면을 따로 그리지 않는다
- edge-to-edge 를 켠다. 인셋은 공용 화면 틀이 처리한다
- Context 는 생성자로 받는다. ViewModel, UseCase 시그니처에 Context 를 쓰지 않는다
- Android 호스트 테스트에는 실제 SQLite 가 없다. DB 테스트는 iOS 시뮬레이터 테스트에 둔다
