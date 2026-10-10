#!/usr/bin/env python3
"""내장 문항 풀(pool.json)의 질문을 Gemini 음성으로 읽어 files/voices/<문항 id>.m4a 로 둔다.

이미 있는 파일은 건너뛰어서 문항을 더하거나 고친 뒤 다시 돌리면 바뀐 것만 부른다. 고친 문항은 그 파일을 지우고 돌린다.
키는 환경 변수 GEMINI_API_KEY 에서만 읽는다. 429 를 받으면 1분 기다렸다 다시 보내고, 부른 횟수와 받은 오디오 토큰을 마지막에 찍는다.
앱의 음성(apps/opic/shared 의 QuestionVoice)과 같은 모델, 같은 목소리를 쓴다. 모노 AAC 48kbps 로 줄이려고 ffmpeg 가 있어야 한다.

사용: GEMINI_API_KEY=... python3 packs/opic-en/scripts/make_question_voices.py [최대 호출 수]
"""
import base64
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

MODEL = "gemini-3.8-flash-tts"
VOICE = "Kore"
ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/commonMain/composeResources/files"


def speak(key: str, text: str) -> tuple[bytes, int]:
    body = {
        "contents": [{"parts": [{"text": text}]}],
        "generationConfig": {"responseModalities": ["AUDIO"], "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": VOICE}}}},
    }
    request = urllib.request.Request(
        f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent",
        data=json.dumps(body).encode(),
        headers={"x-goog-api-key": key, "Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        result = json.load(response)
    audio = base64.b64decode(result["candidates"][0]["content"]["parts"][0]["inlineData"]["data"])
    return audio, result.get("usageMetadata", {}).get("candidatesTokenCount", 0)


def main() -> None:
    key = os.environ.get("GEMINI_API_KEY") or sys.exit("GEMINI_API_KEY 환경 변수가 필요하다")
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 100
    out = ROOT / "voices"
    out.mkdir(exist_ok=True)
    pool = json.loads((ROOT / "questions/pool.json").read_text())
    calls = tokens = 0
    for question in pool:
        target = out / f"{question['id']}.m4a"
        while not target.exists():
            if calls >= limit:
                sys.exit(f"호출 {calls}번에서 멈췄다. 남은 문항은 다시 돌리면 이어서 만든다")
            calls += 1
            try:
                wav, used = speak(key, question["text"])
            except urllib.error.HTTPError as e:
                if e.code != 429 and e.code < 500:
                    raise
                print(f"{question['id']}: HTTP {e.code}, 1분 뒤 다시 보낸다", flush=True)
                time.sleep(60)
                continue
            tokens += used
            with tempfile.NamedTemporaryFile(suffix=".wav") as raw:
                raw.write(wav)
                raw.flush()
                subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", raw.name, "-ac", "1", "-c:a", "aac", "-b:a", "48k", str(target)], check=True)
            print(f"{question['id']}: {used} 토큰", flush=True)
    print(json.dumps({"calls": calls, "audioTokens": tokens}))


if __name__ == "__main__":
    main()
