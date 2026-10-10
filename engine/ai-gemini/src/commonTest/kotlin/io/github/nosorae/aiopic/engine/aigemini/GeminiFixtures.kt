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

    /**
     * 출제 응답(2026-10-10, gemini-3.8-flash). 프롬프트는 questions.md(v2), 수준 2, 서베이 주제 12개, 약점 routine 과 past-experience,
     * 순서는 연습 문항(0번)과 12문항이다.
     */
    const val QUESTIONS = """
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "{\n  \"questions\": [\n    {\n      \"number\": 0,\n      \"type\": \"self-intro\",\n      \"topic\": \"self\",\n      \"text\": \"Welcome to the test. Let's start with a warm-up. Could you tell me a little bit about yourself, such as your name and where you live?\"\n    },\n    {\n      \"number\": 1,\n      \"type\": \"self-intro\",\n      \"topic\": \"self\",\n      \"text\": \"Now, let's begin the actual interview. Please introduce yourself in more detail. Tell me about your daily life, your family, or your hobbies.\"\n    },\n    {\n      \"number\": 2,\n      \"type\": \"description\",\n      \"topic\": \"cafes\",\n      \"text\": \"You indicated that you like going to cafes. Tell me about your favorite cafe. What does it look like inside, and what do you like to drink there?\"\n    },\n    {\n      \"number\": 3,\n      \"type\": \"routine\",\n      \"topic\": \"cafes\",\n      \"text\": \"When do you usually go to a cafe, and who do you go with? Tell me what you normally do when you are there.\"\n    },\n    {\n      \"number\": 4,\n      \"type\": \"past-experience\",\n      \"topic\": \"cafes\",\n      \"text\": \"Tell me about a memorable time you went to a cafe. Who were you with, and what happened there?\"\n    },\n    {\n      \"number\": 5,\n      \"type\": \"description\",\n      \"topic\": \"parks\",\n      \"text\": \"You also mentioned in the survey that you visit parks. Please describe a park you like to go to. Where is it located, and what features does it have?\"\n    },\n    {\n      \"number\": 6,\n      \"type\": \"routine\",\n      \"topic\": \"parks\",\n      \"text\": \"What is your typical routine when you go to the park? Tell me step by step what you do from the time you arrive until you leave.\"\n    },\n    {\n      \"number\": 7,\n      \"type\": \"past-experience\",\n      \"topic\": \"parks\",\n      \"text\": \"Can you share an experience you had at a park recently? When did you go, and what did you do there?\"\n    },\n    {\n      \"number\": 8,\n      \"type\": \"description\",\n      \"topic\": \"cooking\",\n      \"text\": \"You noted that you like cooking. Describe your kitchen at home. What appliances or tools do you use the most?\"\n    },\n    {\n      \"number\": 9,\n      \"type\": \"routine\",\n      \"topic\": \"cooking\",\n      \"text\": \"Tell me about your cooking routine. How often do you cook, and what simple dish do you usually prepare?\"\n    },\n    {\n      \"number\": 10,\n      \"type\": \"past-experience\",\n      \"topic\": \"cooking\",\n      \"text\": \"Talk about a special meal you cooked in the past. What did you make, and how did it turn out?\"\n    },\n    {\n      \"number\": 11,\n      \"type\": \"roleplay-ask\",\n      \"topic\": \"movies\",\n      \"text\": \"I'd like to give you a situation and ask you to act it out. You want to see a movie this weekend with a friend. Call the movie theater and ask three or four questions about movie showtimes and tickets.\"\n    },\n    {\n      \"number\": 12,\n      \"type\": \"roleplay-solve\",\n      \"topic\": \"movies\",\n      \"text\": \"There is a problem you need to resolve. You bought tickets for the movie, but you suddenly cannot go because of an emergency. Call your friend, explain why you cannot make it, and suggest two alternatives.\"\n    }\n  ]\n}",
            "thoughtSignature": "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"
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 554,
    "candidatesTokenCount": 890,
    "totalTokenCount": 1988,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 554
      }
    ],
    "thoughtsTokenCount": 544,
    "serviceTier": "standard"
  },
  "modelVersion": "gemini-3.8-flash",
  "responseId": "L5zJaoLrFeOw0-kP-OnQ6Ao"
}
"""
}
