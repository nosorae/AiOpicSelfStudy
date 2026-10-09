# 개발 그래프

`scripts/graph/render.py` 가 `dev-graph.json` 에서 만든다. 손으로 고치지 않는다.

노드 41개: todo 4, doing 1, done 36, blocked 0. 색은 회색 todo, 노랑 doing, 초록 done, 빨강 blocked 이다.

```mermaid
flowchart TD
  subgraph P0["P0 작업 환경 깔기"]
    h0["h0 #1<br/>사람: 초기 프로젝트 셋업"]
    h1["h1 #2<br/>뼈대 정리와 GitHub 올리기"]
    h2["h2 #3<br/>CLAUDE.md, 소스셋별 규칙"]
    h3["h3 #4<br/>검증 명령 하나, 훅, CI"]
    h4["h4 #5<br/>그래프, 이슈, 프로젝트 로드맵"]
    g0["g0 #6<br/>사람: 하네스 확인"]
  end
  subgraph P1["P1 미리 확인"]
    s1["s1 #9<br/>Gemini 오디오 전송과 실제 청구액"]
    s2["s2 #10<br/>기기 음성으로 질문 읽기 품질"]
    s3["s3 #11<br/>iOS 화면 꺼진 채 녹음 이어 가기"]
    s4["s4 #50<br/>Montage 화면 위 네이티브 탭과 뒤로가기"]
    g1["g1 #12<br/>확인: 미리 확인 결과를 설계에 반영"]
  end
  subgraph P2["P2 공통 뼈대 먼저"]
    c1["c1 #13<br/>도메인 모델"]
    c2["c2 #14<br/>인터페이스"]
    c3["c3 #15<br/>오픽 팩 틀: 프롬프트, 스키마, 등급 체계"]
    c5["c5 #51<br/>Montage 토큰, 아이콘, 컴포넌트 이식"]
    c4["c4 #16<br/>본보기 기능: 키 입력"]
    g2["g2 #17<br/>확인: 공통 뼈대 리뷰"]
  end
  subgraph P3["P3 기능 동시 구현"]
    n_state["n-state #18<br/>시험 진행 상태 계산"]
    n_db["n-db #19<br/>Room 3 저장소"]
    n_gemini["n-gemini #20<br/>Gemini 라이브러리, 어댑터, 모델 목록, 출력 검사"]
    n_route["n-route #52<br/>분석 경로 고르기와 두 단계 경로"]
    n_key["n-key #21<br/>키 저장 (Keystore, Keychain), 제공사별"]
    n_tts["n-tts #22<br/>기기 음성, 단조 시계"]
    n_rec_a["n-rec-a #23<br/>Android 조각 녹음과 끼어듦"]
    n_rec_i["n-rec-i #24<br/>iOS 조각 녹음과 끼어듦"]
    n_queue["n-queue #25<br/>분석 대기열 (WorkManager, iOS)"]
    n_quest["n-quest #26<br/>출제, 내장 문항 풀, 파일 불러오기"]
    n_weak["n-weak #27<br/>약점 점수"]
    n_eval["n-eval #28<br/>품질 확인 도구 세 가지"]
  end
  subgraph P4["P4 화면"]
    u_onb["u-onb #29<br/>시작 분기, 키 입력, 음성 전송 동의"]
    u_shell["u-shell #53<br/>앱 틀: 하단 탭 셋과 뒤로가기"]
    u_exam["u-exam #30<br/>모의시험 화면"]
    u_single["u-single #31<br/>한 문항 연습"]
    u_report["u-report #32<br/>리포트와 기록"]
    u_flag["u-flag #33<br/>틀린 교정 표시와 내보내기"]
    u_model["u-model #54<br/>설정: 분석 모델 고르기, 제공사별 키 관리, 화면 모드"]
    g3["g3 #34<br/>확인: 화면을 목업과 대조"]
  end
  subgraph P5["P5 마무리"]
    e2e["e2e #35<br/>확인: 에뮬레이터, 시뮬레이터로 한 회"]
    docs["docs #36<br/>ADR 25개, README AI 사용 범위, 모듈별 README, 오류 분석"]
    store["store #37<br/>스토어 준비: 개인정보처리방침, Data safety, 마이크 서비스 신고"]
    g4["g4 #38<br/>완료 보고"]
  end
  h0 --> h1
  h1 --> h2
  h1 --> h3
  h1 --> h4
  h2 --> g0
  h3 --> g0
  h4 --> g0
  g0 --> s1
  g0 --> s2
  g0 --> s3
  g0 --> s4
  s1 --> g1
  s2 --> g1
  s3 --> g1
  s4 --> g1
  g1 --> c1
  c1 --> c2
  c1 --> c3
  g1 --> c5
  c2 --> c4
  c5 --> c4
  c3 --> g2
  c4 --> g2
  g2 --> n_state
  g2 --> n_db
  g2 --> n_gemini
  g2 --> n_route
  g2 --> n_key
  g2 --> n_tts
  g2 --> n_rec_a
  g2 --> n_rec_i
  n_gemini --> n_queue
  n_db --> n_queue
  n_route --> n_queue
  n_gemini --> n_quest
  n_db --> n_quest
  n_db --> n_weak
  n_gemini --> n_eval
  n_key --> u_onb
  n_queue --> u_onb
  g2 --> u_shell
  n_state --> u_exam
  n_rec_a --> u_exam
  n_rec_i --> u_exam
  n_tts --> u_exam
  n_quest --> u_exam
  u_exam --> u_single
  n_weak --> u_single
  n_queue --> u_report
  n_weak --> u_report
  u_report --> u_flag
  n_gemini --> u_model
  n_key --> u_model
  n_route --> u_model
  u_shell --> u_model
  u_onb --> g3
  u_shell --> g3
  u_single --> g3
  u_flag --> g3
  u_model --> g3
  g3 --> e2e
  g3 --> docs
  n_eval --> docs
  g3 --> store
  e2e --> g4
  docs --> g4
  store --> g4
  classDef todo fill:#eeeeee,stroke:#999999,color:#000
  class e2e,docs,store,g4 todo
  classDef doing fill:#fff3b0,stroke:#d4a017,color:#000
  class g3 doing
  classDef done fill:#c8f7c5,stroke:#2e7d32,color:#000
  class h0,h1,h2,h3,h4,g0,s1,s2,s3,s4,g1,c1,c2,c3,c5,c4,g2,n_state,n_db,n_gemini,n_route,n_key,n_tts,n_rec_a,n_rec_i,n_queue,n_quest,n_weak,n_eval,u_onb,u_shell,u_exam,u_single,u_report,u_flag,u_model done
  classDef blocked fill:#ffcdd2,stroke:#c62828,color:#000
```

## 노드 안쪽 루프

모든 노드가 같은 반복을 돈다. `dev-graph.json` 의 `loop` 가 원본이고 노드의 `loop` 가 횟수 상한을 덮어쓴다. done 이 `human` 인 노드는 사람이 확인한다.

```mermaid
flowchart LR
  L_implement["구현"]
  L_done["done 명령"]
  L_review["리뷰"]
  L_merge["병합"]
  L_blocked(["blocked: 사람"])
  L_implement --> L_done
  L_done -->|pass| L_review
  L_done -. 넘으면 .-> L_blocked
  L_done -->|fail 최대 3번| L_implement
  L_review -->|approve| L_merge
  L_review -. 넘으면 .-> L_blocked
  L_review -->|reject 최대 2번| L_implement
```
