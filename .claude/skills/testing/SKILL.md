---
name: testing
description: 이 앱의 테스트 규칙. 손으로 짠 Fake, 끝단(HTTP)에서만 모킹, Main 디스패처 교체, ViewModel 테스트(onIntent 뒤 uiState.value), kotlin.test, 백틱 한국어 이름, 스크린샷 테스트. 테스트를 쓰거나 고칠 때 먼저 읽는다.
---

# 테스트

테스트를 먼저 쓰고, 실패하는 것을 본 뒤 구현한다. 완료는 `./gradlew check` 의 종료 코드로 판정한다.

## 1. 어디에 두나

| 무엇 | 소스셋 | 명령 |
|---|---|---|
| 순수 로직, ViewModel, UseCase | `commonTest` | `./gradlew :모듈:allTests` |
| Android 구현 클래스 | `androidHostTest` | `./gradlew :모듈:testAndroidHostTest` |
| Room, iOS 구현 클래스 | `iosTest` | `./gradlew :모듈:iosSimulatorArm64Test` |
| 화면 모양 | 스크린샷 테스트 | 아래 5절 |

Android 호스트 테스트에는 실제 SQLite 가 없다. DB 테스트는 iOS 시뮬레이터에서 돌린다.

## 2. 테스트 대역

- **Fake 를 손으로 짠다.** 인터페이스를 메모리로 구현한 클래스다(`FakeAnswerRepository`). 상태를 들고 있어서 "저장한 뒤 읽으면 나온다"를 그대로 확인한다
- **MockK, Mockito, Turbine 을 쓰지 않는다.** 호출 횟수 대신 결과 상태를 본다
- **모킹은 끝단에서만 한다.** Gemini 요청은 Ktor `MockEngine` 에 저장해 둔 실제 응답을 넣어 확인한다. 네트워크 없이 돈다
- Fake 는 처음엔 그 모듈의 `commonTest` 에 둔다. 두 번째 모듈이 같은 Fake 를 쓸 때 공용 테스트 모듈로 옮긴다
- ViewModel 테스트에는 진짜 UseCase 에 Fake 저장소를 넣는다. UseCase 를 Fake 로 바꾸지 않는다

## 3. 코루틴과 시간

commonTest 에서는 JUnit Rule 을 못 쓴다. Main 디스패처는 `@BeforeTest`, `@AfterTest` 로 바꾼다.

```kotlin
class ExamViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()
}
```

- `runTest(dispatcher)` 안에서 `advanceUntilIdle()`, `advanceTimeBy()` 로 시간을 돌린다
- UseCase 에 주입하는 디스패처도 같은 `dispatcher` 를 넣는다
- 시계는 Fake `Clock`, `TestTimeSource` 를 주입한다. 40분 타이머, 녹음 10초 규칙을 실제로 기다리지 않는다

## 4. ViewModel 테스트

`onIntent` 를 부르고 `uiState.value` 만 본다. 일회성 이벤트도 상태 필드라서 따로 수집할 것이 없다.

```kotlin
@Test
fun `키 확인에 성공하면 동의 화면으로 이동한다`() = runTest(dispatcher) {
    val viewModel = KeyViewModel(verifyKeyUseCase = VerifyKeyUseCase(FakeKeyVault(), FakeGemini(valid = true)))

    viewModel.onIntent(KeyIntent.SubmitKey("abc"))
    advanceUntilIdle()

    assertEquals(KeyDestination.Consent, viewModel.uiState.value.navigation)
}
```

- 단언은 `kotlin.test` 만 쓴다(`assertEquals`, `assertIs`, `assertTrue`)
- 테스트 이름은 백틱으로 감싼 한국어 문장이다. "무엇을 하면 무엇이 된다"로 쓴다
- 한 테스트에 확인 하나. 준비, 실행, 확인 사이를 빈 줄로 나눈다
- 요구사항을 확인하는 테스트는 이름이나 주석에 번호(F2.4 등)를 단다

## 5. 스크린샷 테스트

- 안쪽 화면(`XxxScreen(state, onIntent)`)에 고정 UiState 를 넣어 찍는다. `@Preview` 와 같은 입력을 쓴다
- 상태마다 한 장(빈 상태, 로딩, 오류, 녹음 중 등)
- 움직이는 컴포넌트는 멈춘 첫 프레임으로 그린다. 찍을 때마다 그림이 달라지면 안 된다
- 기준 이미지는 저장소에 커밋하고, 바뀌면 PR 에 전후 이미지를 붙인다
- iOS 스크린샷은 사람이 보는 증거로만 쓰고 자동 비교 실패로 판정하지 않는다

## 6. 시험 진행 상태

진행 계산 순수 함수는 상태와 일어난 일의 조합을 모두 단위 테스트로 본다. 앱 꺼짐, 40분 끝, 7번 뒤 재조정, 수준 1~2 의 12문항이 중심이다.

## 7. 테스트가 실제로 잡는지

구현을 마치면 일부러 결함 하나를 넣어 테스트가 실패하는지 보고 되돌린다. 넣은 결함과 실패한 단언을 PR 의 확인 절에 적는다.
