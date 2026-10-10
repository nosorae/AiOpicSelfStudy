# 0028. 회차 등급은 하한으로 정하고 Advanced 체크는 문단으로만 센다

- 상태: 채택
- 날짜: 2026-10-10

## 배경

품질 확인 보고서에서 IM2 수준으로 쓴 대본이 같은 입력 10번 모두 IM3~IH 를 받았다(`opic-en-session-grade-v1`). 등급 이름과 수준 정의는 ACTFL 것이라 체크리스트 문장이 그 정의와 맞아야 한다. v1 을 정의와 대조하니 네 곳이 어긋났다.

| v1 의 문장과 규칙 | 등급 정의 | 어긋난 결과 |
|---|---|---|
| C5 "과거 일을 과거 시제로 대체로 맞게 말한다" | Intermediate 는 문장이나 문장 나열로 말하고, 문장을 문단으로 엮지 못한다. Advanced 는 시작, 중간, 끝이 있는 문단으로 이야기한다 | 시제만 맞는 문장 나열이 Advanced 근거로 셌다 |
| C8 "롤플레이에서 문제를 설명하고 대안을 낸다" | Advanced 기능은 문단 길이로 복잡한 상황을 처리하는 것이다 | "못 간다. 감기다. 동생이랑 가라" 같은 짧은 문장도 C8 을 채웠다 |
| C5, C6, C8 이 맞으면 IH 가능 | IH 는 Advanced 과제를 대부분 해내고 문단 길이로 자주 서술하지만 어딘가에서 무너진다 | 문단(C7) 없이도 IH 범위가 열렸다 |
| 모든 문장에 "거의 모든 답변에서" 하나의 기준 | 등급은 모든 답변에서 유지하는 수준(하한)으로 정하고, 한 단계 위 과제에서 무너지는 지점(상한)은 하위 등급만 움직인다. IH 는 위 수준을 "대부분" 하는 것이지 "모두"가 아니다 | 하한 규칙이 없어 위 수준 근거가 조금만 보여도 범위가 올라갔다 |

IM1, IM2, IM3 의 세분은 한국 시행분에만 있고, 운영사 공개 자료에는 "Fluency, Delivery, Production 을 기준으로" 나눈다는 문장만 있다. 세분의 구체 기준은 공개되지 않았다.

## 후보

1. v1 체크 문장을 그대로 두고 대응 규칙의 경계만 한 칸씩 올린다
2. 체크를 Intermediate(C1~C4), Advanced(C5~C8), 유지(C9, C10)로 나누고 Advanced 체크는 문단으로 해낸 것만 세며, 범위의 아래쪽은 하한으로 정한다

## 결정

2를 고른다. 프롬프트는 `opic-en-session-grade-v2` 다.

- C1~C4 는 해당하는 모든 답변에서 맞아야 예다. 하한을 정하는 체크다
- C5~C8 은 그 과제를 묻는 답변 대부분에서 문단으로 해냈을 때만 예다. 문장 수준 답은 시제와 내용이 맞아도 세지 않는다
- C9 는 C5~C8 을 모든 답변에서 유지했는지, C10 은 학습자를 대해 보지 않은 사람이 힘들이지 않고 알아듣는지다
- 대응: C1, C2 가 아니면 Novice, C3 이나 C4 가 아니면 IL, C1~C4 면 IM, C1~C8 이고 C9 가 아니면 IH, 모두 맞으면 AL 이다
- IM 안에서는 C5~C8 이 하나도 없으면 IM2, 일부 맞으면 IM3, 문장 나열이 짧고 멈춤이 많으면 IM1 이다. IM 세분 기준이 공개되지 않아 이 규칙은 이 앱이 정한 것이다. 위 수준 근거가 일부 보이면 위쪽 IM 이라는 방향만 ACTFL 의 Mid 정의("다음 주 등급 과제를 부분적으로 해낼 수 있다")와 맞춘다
- 범위의 high 는 low 보다 한 등급 위까지만 준다

## 이유

1을 고르면 경계만 옮길 뿐 문장 나열과 문단을 가르지 못해 IM2 대본과 IH 대본이 같은 체크 결과를 받는다. 체크가 문단 여부를 보게 해야 "어느 문항에서 무너졌나"(ADR 0011)를 근거와 함께 보여 줄 수 있다. 범위 폭을 한 등급으로 묶은 것은 IM3~IH 처럼 넓은 범위가 연습 목표를 흐리게 해서다.

v2 로 같은 입력을 다시 돌리니 IM2 대본은 5번 모두 IM2~IM3 이고, NL 대본 NL~NM, AL 대본 AL~AL 로 순서가 유지됐다. 일부러 넣은 오류를 잡은 수는 v1 과 같은 4/5 다.

## 결과

- 시험 팩 응답 스키마와 체크 번호(C1~C10)는 그대로라 앱 코드는 바뀌지 않는다
- 저장된 결과의 `promptVersion` 으로 v1 과 v2 등급을 구분한다. v1 결과는 다시 매기지 않는다
- IM 세분 규칙은 사용자 표시와 실제 성적이 모이면 다시 본다. 평가 대본은 네 문항짜리라 15문항 회차의 C9 판정은 아직 확인하지 못했다

## 근거

- [회차 등급 프롬프트](../../packs/opic-en/src/commonMain/composeResources/files/prompts/session-grade.md)
- [품질 확인 보고서 v1](../../evals/reports/opic-en-session-grade-v1_opic-en-answer-v2_gemini-3.8-flash.md), [v2](../../evals/reports/opic-en-session-grade-v2_opic-en-answer-v2_gemini-3.8-flash.md)
- [오류 분석](../error-analysis.md)
- 등급 정의 출처. ACTFL Proficiency Guidelines 2024(Speaking), 12~13쪽 하위 등급 정의, 17~18쪽 Intermediate Mid, Intermediate High, 20쪽 Advanced Low: https://www.oregon.gov/ode/students-and-family/equity/EngLearners/Documents/ACTFL-Proficiency-Guidelines-2024.pdf
- ACTFL OPIc Familiarization Guide 2024, 6~7쪽 평가 기준 표(Intermediate 는 Sentences, Advanced 는 Oral paragraphs)와 holistic 판정: https://www.languagetesting.com/pub/media/wysiwyg/PDF/ACTFL-OPIc-familiarization-guide.pdf
- ACTFL OPIc Examinee Handbook 2018, 15~17쪽 Intermediate, Advanced, 하위 등급 설명: https://www.languagetesting.com/pub/media/wysiwyg/PDF/opic-examinee-handbook.pdf
- ACTFL OPIc ACE 보고서 2020, 하한(floor)과 상한(ceiling)으로 판정하는 문항 설계와 시험 전체에 등급 하나를 주는 방식: https://www.actfl.org/uploads/files/general/Documents/assessments/acereports/AAR-OPIc-ACE-I-2020-001.pdf
- OPIc 한국 운영사 등급 안내(IM3 > IM2 > IM1 세분): https://www.opic.or.kr/opics/servlet/controller.opic.site.about.AboutServlet?p_process=move-introduce-opic
- OPIc 가이드북 2016, 13쪽 "Intermediate Mid의 경우 Fluency, Delivery, Production을 기준으로 Mid3(상), Mid2(중), Mid1(하)로 세분화": http://www.opic.or.kr/senior/mail/2016/0729/images/a/opic_guide.pdf
