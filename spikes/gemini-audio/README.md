# Gemini 오디오 전송 실험 (s1)

60초 영어 답변 음성을 `gemini-3.8-flash` 에 보내 조각 전송, 구조화 출력, 단어 시각, 비용을 쟀다. 결과는 `result.json` 이다. 코드는 실험용이라 앱에 옮기지 않는다.

음성은 macOS `say` 합성 음성이다. 사람 녹음이 아니라서 발음, 머뭇거림, 잡음 조건은 이 결과에 없다.

결과 요약(2026-10-09, 요청 18번):

- 5초 조각 12개를 한 요청에 inline 으로 넣어도, 한 파일로 보내도 전문 단어 오류율 0%였다
- 오디오 입력과 `generationConfig.responseFormat.text`(`mimeType: "APPLICATION_JSON"`, `schema`)를 한 요청에 같이 쓸 수 있다. `"application/json"` 을 넣으면 400 이다
- 일반 모델이 돌려준 단어 시각으로 찾은 멈춤은 실제 위치와 0.45초 안, `gemini-3.5-transcribe` 는 0.04초 안이었다
- 답변 1개 분석 약 $0.013, 15문항 회차 전체(답변 15개와 등급 1번) 약 $0.23(2027년 단가 $0.46). 무료 등급이라 실제 청구액은 0 이다
- 무료 등급 `gemini-3.8-flash` 는 하루 20회에서 429 를 받았고, 18번 중 8번이 503 과부하였다

다시 돌리기:

```bash
./make_audio.sh                          # audio/ 를 다시 만든다
GEMINI_API_KEY=... python3 spike.py b_full   # 실험 이름은 spike.py 아래쪽 분기 참고. raw/ 에 응답 본문만 남는다
python3 analyze.py                       # raw/ 로 result.json 을 만든다. 네트워크를 쓰지 않는다
```
