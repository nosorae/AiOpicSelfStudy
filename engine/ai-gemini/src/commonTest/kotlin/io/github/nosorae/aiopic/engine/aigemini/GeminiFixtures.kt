package io.github.nosorae.aiopic.engine.aigemini

/**
 * Gemini API 가 실제로 돌려준 응답 본문(2026-10-10, gemini-3.5-flash-lite). 헤더와 키는 남기지 않았다.
 * 녹음은 `spikes/gemini-audio/audio/full.m4a`, 프롬프트는 `packs/opic-en` 의 answer.md 와 session-grade.md(v1), 스키마는 그 팩의 응답 클래스다.
 * 회차 등급은 같은 답변 피드백 셋과 같은 녹음 셋으로 보냈다.
 */
internal object GeminiFixtures {
    const val ANSWER = """
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "{\n  \"transcript\": \"Hi, my name is Alex and I am a software developer living in Seoul. I usually work from home three days a week, and on the other days I go to an office near Gangnam station. In my free time I like to go jogging along the Han River, especially in the early morning when the air is fresh. After jogging I usually make a simple breakfast with eggs and toast. \\nLast weekend I went to a small cafe with my friends. We talked about our trip to Busan last summer. We ate a lot of seafood and we walked on the beach until midnight. It was one of the best trips I have ever had. I took so many pictures that my phone ran out of storage. \\nThese days I am trying to improve my English speaking skills because I want to work with people from other countries. I practice every evening for about 30 minutes, and I think my confidence is getting better little by little. My goal is to get a good score on my speaking test next month.\",\n  \"corrections\": [],\n  \"upgrades\": [],\n  \"tenseConsistent\": true,\n  \"sentenceCount\": 16,\n  \"wordsPerMinute\": 141.0,\n  \"pauses\": [],\n  \"pronunciation\": \"The speaker's pronunciation is very clear with natural intonation. The speech flows smoothly without noticeable issues.\",\n  \"score\": 100\n}",
            "thoughtSignature": "EmAKXgFpFH0TtKxGFtqLl+Vfqt4sLfDOkYnImn09D9fKHBrFqb8uoM6SDRQYDNOG91OdYp8CkKgNRSUBhdjPXFdK3wizHp90B4oJnaUUIZlRvFOU5oeIO8OEBhyexP2Al00="
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 1831,
    "candidatesTokenCount": 304,
    "totalTokenCount": 2135,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 352
      },
      {
        "modality": "AUDIO",
        "tokenCount": 1479
      }
    ],
    "serviceTier": "standard"
  },
  "modelVersion": "gemini-3.5-flash-lite",
  "responseId": "1gnJauibOKOq0-kP1uvXoQE"
}
"""

    const val SESSION_GRADE = """
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "{\n  \"checks\": [\n    {\n      \"id\": \"C1\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C2\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C3\",\n      \"met\": false\n    },\n    {\n      \"id\": \"C4\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C5\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C6\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C7\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C8\",\n      \"met\": false\n    },\n    {\n      \"id\": \"C9\",\n      \"met\": true\n    },\n    {\n      \"id\": \"C10\",\n      \"met\": true\n    }\n  ],\n  \"low\": \"IH\",\n  \"high\": \"IH\",\n  \"reasons\": [\n    \"The speaker consistently produces paragraph-length responses with connected sentences, discusses past, present, and future correctly, and maintains clear pronunciation without role-play questions/alternatives (C3/C8 not met).\"\n  ],\n  \"brokeAt\": []\n}",
            "thoughtSignature": "EmAKXgFpFH0T4QnhuBAF6FcPlDWIIA7eiJtnAAa4uWPI4+OOyo0ouNAMxhAmGrkQaT62MeKesRFuT+adQvGGoX0uOfgoMMWNq1gacwtvCfYlrLLJnx/d3RC0L3boF6h7JqY="
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 5724,
    "candidatesTokenCount": 301,
    "totalTokenCount": 6025,
    "promptTokensDetails": [
      {
        "modality": "AUDIO",
        "tokenCount": 4437
      },
      {
        "modality": "TEXT",
        "tokenCount": 1287
      }
    ],
    "serviceTier": "standard"
  },
  "modelVersion": "gemini-3.5-flash-lite",
  "responseId": "3wnJasb6O63a9tMPu7qmgQU"
}
"""
}
