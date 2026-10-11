# llm-gemini

Gemini `generateContent` 를 부르는 HTTP 클라이언트다. 프롬프트, 녹음, 응답 스키마를 받아 모델이 돌려준 글을 주고, 음성 합성 모델로 질문 음성을 받는다. 분석 포트와 프롬프트 내용은 모른다. 그것을 잇는 어댑터는 `engine/ai-gemini` 에 있다([ADR 0019](../../docs/adr/0019-AI-분석은-받아쓰기와-글-분석-두-단계-포트.md)). 프로젝트의 다른 모듈을 의존하지 않는다.

```kotlin
val client = GeminiClient() // 테스트는 MockEngine 을 넣은 HttpClient 를 준다
when (val result = client.generate(LlmRequest(apiKey, model, prompt, audio, responseSchema))) {
    is LlmResult.Success -> result.text
    is LlmResult.Retryable -> TODO("기다렸다 다시 보낸다")
    LlmResult.InvalidKey -> TODO("설정 화면으로 보낸다")
    LlmResult.ModelNotFound -> TODO("기본 모델로 돌린다")
    is LlmResult.Rejected -> TODO("다시 보내도 같다")
}
```

- `speech(apiKey, model, voice, text)` 는 글을 읽은 오디오(24kHz 모노 16비트 WAV)를 준다. 실패는 모두 null 이다. 부르는 쪽이 기기 음성으로 읽어서 이유를 나누지 않는다
- 실패는 예외 대신 `LlmResult` 로 돌려준다. 429, 408, 5xx, 연결 없음은 `Retryable` 이다
- JSON 응답이 필요하면 `responseSchema` 를 늘 준다. 스키마 없이 JSON 을 요청하면 마크다운 표가 올 때가 있다
- `LlmRequest.thinkingLevel` 로 생각 수준을 낮추면 답이 빨리 온다. 없으면 모델 기본값이다
- `GeminiClient(http, upload)` 의 `upload` 가 있으면 녹음을 실은 요청은 그 길로 보낸다. iOS 는 `BackgroundUpload(identifier)` 를 준다. 앱이 멈춰도 시스템이 전송을 끝내고, 같은 요청이 진행 중이면 다시 보내지 않고 그 결과를 기다린다. 앱 틀은 `handleEventsForBackgroundURLSession` 의 처리기를 `whenEventsDelivered` 로 넘긴다
- `LlmRequest.toString()` 은 키를 찍지 않는다

## 테스트

`commonTest` 의 `GeminiClientTest` 가 Ktor `MockEngine` 으로 요청 본문과 상태 코드별 결과를 확인한다. `iosTest` 의 `BackgroundUploadTest` 는 시스템 전송 대신 가짜를 넣어 다시 보내지 않기와 늦게 온 응답 남기기를 본다. 실제 Gemini 는 부르지 않는다. 오류 응답은 `GeminiErrorFixtures` 에 저장해 둔 본문을 쓴다.
