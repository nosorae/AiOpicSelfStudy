# 품질 확인

- 모델: gemini-3.8-flash
- 답변 프롬프트: opic-en-answer-v2
- 회차 등급 프롬프트: opic-en-session-grade-v2

## 같은 입력 5번 (IM2 수준 회차)

| 회 | 등급 범위 |
|---|---|
| 1 | IM2~IM3 |
| 2 | IM2~IM3 |
| 3 | IM2~IM3 |
| 4 | IM2~IM3 |
| 5 | IM2~IM3 |

같은 범위: 5/5 (IM2~IM3)
답이 갈린 체크: 없음

## 일부러 틀린 답

| 넣은 오류 | 맞는 말 | 잡았나 |
|---|---|---|
| I go to Busan | I went to Busan | 예 |
| We stay at | We stayed at | 예 |
| I eat a lot | I ate a lot | 예 |
| a amazing trip | an amazing trip | 예 |
| a umbrella | an umbrella | 아니오 |

잡은 비율: 4/5

## 수준 다른 답 셋

| 대본 수준 | 받은 등급 범위 |
|---|---|
| NL | NL~NM |
| IM2 | IM2~IM3 |
| AL | AL~AL |

등급 순서: 맞다
