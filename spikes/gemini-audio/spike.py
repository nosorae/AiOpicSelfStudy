"""Gemini 오디오 전송 실험. 키는 환경 변수 GEMINI_API_KEY 에서만 읽고 어디에도 쓰지 않는다.
사용: python3 spike.py <실험이름> ; 응답 본문만 raw/<실험이름>.json 에 남긴다(헤더, 키 없음)."""
import base64, glob, json, os, sys, time, urllib.error, urllib.request

KEY = os.environ.get("GEMINI_API_KEY") or sys.exit("GEMINI_API_KEY 환경 변수가 필요하다")
BASE = "https://generativelanguage.googleapis.com/v1beta"
MODEL = "gemini-3.8-flash"
HERE = os.path.dirname(os.path.abspath(__file__))
os.makedirs(f"{HERE}/raw", exist_ok=True)


def call(name, url, body):
    req = urllib.request.Request(url, json.dumps(body).encode(), {"Content-Type": "application/json", "x-goog-api-key": KEY})
    t = time.time()
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            out, status = json.load(r), r.status
    except urllib.error.HTTPError as e:
        out, status = json.loads(e.read() or b"{}"), e.code
    out["_spike"] = {"status": status, "seconds": round(time.time() - t, 2), "model": body.get("model") or url.split("/models/")[-1].split(":")[0]}
    with open(f"{HERE}/raw/{name}.json", "w") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    with open(f"{HERE}/raw/requests.log", "a") as f:
        f.write(f"{name}\t{status}\n")
    return out


def part(path):
    return {"inlineData": {"mimeType": "audio/mp4", "data": base64.b64encode(open(path, "rb").read()).decode()}}


def gen(name, parts, gen_cfg=None, model=MODEL):
    body = {"contents": [{"role": "user", "parts": parts}]}
    if gen_cfg:
        body["generationConfig"] = gen_cfg
    return call(name, f"{BASE}/models/{model}:generateContent", body)


CHUNKS = sorted(glob.glob(f"{HERE}/audio/chunks/*.m4a"))
FULL = f"{HERE}/audio/full.m4a"
TRANSCRIBE = "Transcribe this English speech verbatim. Output only the transcript text."
SCHEMA = {
    "type": "object",
    "properties": {
        "transcript": {"type": "string"},
        "corrections": {"type": "array", "items": {"type": "object", "properties": {
            "original": {"type": "string"}, "corrected": {"type": "string"}}, "required": ["original", "corrected"]}},
        "wordsPerMinute": {"type": "number"},
        "pauses": {"type": "array", "items": {"type": "object", "properties": {
            "startSeconds": {"type": "number"}, "endSeconds": {"type": "number"}}, "required": ["startSeconds", "endSeconds"]}},
    },
    "required": ["transcript", "corrections", "wordsPerMinute", "pauses"],
}
ANALYZE = ("You are an English speaking coach. Analyze this spoken answer to 'Please introduce yourself.' "
           "Return the verbatim transcript, grammar or wording corrections (original phrase must appear in the transcript), "
           "speaking rate in words per minute, and every silent pause longer than 1 second with start and end time in seconds from the start of the audio.")
WORDS = ("Transcribe this English speech. Return JSON: an array of objects {\"w\": word, \"s\": start seconds, \"e\": end seconds} "
         "for every word, times measured from the start of the audio with 2 decimals.")

exp = sys.argv[1]
if exp == "a_chunks":
    gen(exp, [{"text": TRANSCRIBE + " The audio is split into consecutive chunks in order; join them into one transcript."}] + [part(c) for c in CHUNKS])
elif exp == "b_full":
    gen(exp, [{"text": TRANSCRIBE}, part(FULL)])
elif exp == "c_struct_format":  # 새 필드 responseFormat
    gen(exp, [{"text": ANALYZE}, part(FULL)], {"responseFormat": {"text": {"mimeType": "APPLICATION_JSON", "schema": SCHEMA}}})
elif exp == "c_struct_chunks":
    gen(exp, [{"text": ANALYZE + " The audio is split into consecutive chunks in order; treat them as one recording."}] + [part(c) for c in CHUNKS],
        {"responseFormat": {"text": {"mimeType": "APPLICATION_JSON", "schema": SCHEMA}}})
elif exp == "d_words":
    gen(exp, [{"text": WORDS}, part(FULL)], {"responseMimeType": "application/json"})
elif exp == "e_transcribe":
    call(exp, f"{BASE}/interactions", {
        "model": "gemini-3.5-transcribe",
        "input": [{"type": "audio", "data": base64.b64encode(open(FULL, "rb").read()).decode(), "mime_type": "audio/mp4"}],
        "generation_config": {"transcription_config": {"mode": {"type": "verbatim", "timestamp_granularities": ["word"]}}},
    })
elif exp.startswith("f_session"):  # 회차 등급: 답변 피드백 15개 + 오디오 n개. 인자: n [모델]
    n = int(sys.argv[2])
    fb = json.load(open(f"{HERE}/raw/c_struct_ok.json"))["candidates"][0]["content"]["parts"][0]["text"]
    parts = [{"text": "You grade a 15-question English speaking test session. Below are per-answer feedback JSON and the answer recordings. "
              "Fill an ACTFL checklist with yes/no per statement and give a level range (e.g. IM3 ~ IH) with reasons."}]
    for i in range(15):
        parts.append({"text": f"Answer {i + 1} feedback: {fb}"})
    parts += [part(FULL)] * n
    if exp == "f_session_count":  # 15개 묶음의 정확한 입력 토큰 수(생성 안 함)
        call(exp, f"{BASE}/models/{MODEL}:countTokens", {"contents": [{"role": "user", "parts": parts}]})
    else:  # 무료 등급 일일 한도에 막혀 다른 안정 모델로 묶음 전송을 확인한다
        gen(exp, parts, {"responseFormat": {"text": {"mimeType": "APPLICATION_JSON"}}}, model=sys.argv[3] if len(sys.argv) > 3 else MODEL)
