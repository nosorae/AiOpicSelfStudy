# iOS 화면 꺼진 채 녹음 스파이크

iPhone 16 Pro(iOS 26.6.1)에서 화면을 492초 잠근 동안 5초 조각 98개가 끊기지 않고 생겼다. 녹음된 시간은 잠긴 시간의 98.3% 이고, 중단과 오류는 0건이다. 숫자는 `result.json` 에 있다.

## 방법

앱 본체와 떨어진 일회용 앱이다. 실행하자마자 `.playAndRecord` 세션으로 녹음을 시작하고 5초마다 `Documents/chunk-NNNN.m4a`(AAC 16kHz 모노)를 새로 쓴다. 조각마다 생성 시각과 길이를 `Documents/log.jsonl` 에 남기고, 오디오 중단, 경로 변경, 앱 상태 변화, 화면 잠금(`protectedDataWillBecomeUnavailable`)과 해제도 함께 남긴다. `Info.plist` 의 `UIBackgroundModes` 에 `audio` 를 넣었고, 파일 보호 수준은 잠금 중에도 쓸 수 있는 `completeUntilFirstUserAuthentication` 이다.

화면이 켜진 상태로 녹음이 도는 것을 확인한 뒤 측면 버튼으로 잠그고 8분 넘게 두었다가 풀었다. `analyze.py` 는 잠금과 해제를 짝지어 가장 긴 구간을 본 측정으로 삼는다.

## 결과

| 항목 | 값 |
|---|---|
| 잠긴 시간 | 492초 |
| 잠긴 동안 생긴 조각 | 98개 |
| 잠긴 동안 녹음된 시간 | 483.7초 (98.3%) |
| 조각 사이 최대 간격 | 5.0초 |
| 중단 알림, 오류 | 0건 |

조각 평균 길이는 4.938초다. 조각을 바꿀 때마다 녹음기를 멈추고 새로 여는 사이 약 0.07초가 빠진다. 2분 답변을 5초 조각으로 이으면 약 1.7초가 빠지고, 말 한가운데서 빠지면 음절이 잘린다. 그래서 이어 녹음은 답변 하나를 녹음기 하나로 끊지 않고 녹음한다. 앱이 죽어도 남도록 파일을 나눠야 하면 녹음기는 그대로 두고 받은 버퍼를 쓰는 파일만 바꾼다(`AVAudioEngine` 탭).

잠금을 풀 때 경로 변경 알림(reason 8, 경로 구성 변경)이 두 번 왔지만 녹음은 이어졌다. 이 알림만으로 녹음을 다시 시작하지 않는다.

## 한계

한 번만 쟀다. 이어폰과 블루투스 없이 기기 마이크만 썼다. 전화, 알람, 다른 앱의 오디오 재생 같은 중단은 일으켜 보지 않았다.

## 다시 돌리기

`BgRecord.xcodeproj` 는 커밋하지 않는다. `project.yml` 로 xcodegen 이 다시 만든다. 서명 팀은 앱과 같은 `apps/opic/iosApp/Configuration/Local.xcconfig` 에서 읽는다.

```
xcodegen generate
xcodebuild -project BgRecord.xcodeproj -scheme BgRecord -configuration Debug -destination 'id=<기기 UDID>' -derivedDataPath <빌드 폴더> -allowProvisioningUpdates build
xcrun devicectl device install app --device <devicectl id> <빌드 폴더>/Build/Products/Debug-iphoneos/BgRecord.app
xcrun devicectl device process launch --device <devicectl id> io.github.nosorae.aiopic.spike.bgrecord
```

처음 실행하면 마이크 권한을 허용한다. 화면을 잠그고 5분 넘게 기다린 뒤 풀고 결과를 받는다.

```
xcrun devicectl device copy from --device <devicectl id> --domain-type appDataContainer --domain-identifier io.github.nosorae.aiopic.spike.bgrecord --source Documents --destination <받을 폴더>
python3 analyze.py <받을 폴더>/log.jsonl
```
