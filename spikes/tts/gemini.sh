#!/bin/sh
# 비교 기준용으로 질문 1번만 Gemini TTS 로 읽어 m4a 로 만든다. 키는 환경 변수에서만 읽는다.
# 사용: GEMINI_API_KEY=... sh gemini.sh <출력 m4a>
set -eu
: "${GEMINI_API_KEY:?GEMINI_API_KEY 가 필요하다}"
OUT="$1"; W="$(mktemp -d)"
python3 -c "import json,sys;print(json.dumps({'contents':[{'parts':[{'text':open(sys.argv[1]).readline().strip()}]}],'generationConfig':{'responseModalities':['AUDIO'],'speechConfig':{'voiceConfig':{'prebuiltVoiceConfig':{'voiceName':'Kore'}}}}}))" "$(dirname "$0")/questions.txt" > "$W/req.json"
curl -sf -H "x-goog-api-key: $GEMINI_API_KEY" -H "Content-Type: application/json" -d @"$W/req.json" \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent" \
  | python3 -c "import json,sys,base64;sys.stdout.buffer.write(base64.b64decode(json.load(sys.stdin)['candidates'][0]['content']['parts'][0]['inlineData']['data']))" > "$W/out.pcm"
ffmpeg -loglevel error -y -f s16le -ar 24000 -ac 1 -i "$W/out.pcm" -c:a aac -b:a 48k "$OUT"
rm -rf "$W"
