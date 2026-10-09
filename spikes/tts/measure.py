"""샘플을 Gemini 로 다시 받아써서 원문과 단어 오류율(WER), 분당 단어 수, 문장 사이 쉼을 잰다.

사용: GEMINI_API_KEY=... python3 measure.py > measurements.json
키는 환경 변수에서만 읽고 어디에도 쓰지 않는다.
"""
import base64, json, os, re, subprocess, sys, time, urllib.error, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
MODEL = "gemini-3.5-flash-lite"
THRESHOLD = 0.05  # WER 5% 이하면 연습에 쓸 만하다고 본다


def words(s):
    return re.sub(r"[^a-z0-9' ]", " ", s.lower().replace("’", "'")).split()


def wer(ref, hyp):
    r, h = words(ref), words(hyp)
    d = list(range(len(h) + 1))
    for i in range(1, len(r) + 1):
        prev, d[0] = d[0], i
        for j in range(1, len(h) + 1):
            prev, d[j] = d[j], min(d[j] + 1, d[j - 1] + 1, prev + (r[i - 1] != h[j - 1]))
    return d[len(h)] / len(r)


def duration(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path],
                         capture_output=True, text=True, check=True).stdout
    return float(out)


def inner_pauses(path, dur):
    # -35dB 아래가 0.15초 넘게 이어지면 쉼으로 본다. 처음과 끝의 무음은 뺀다
    log = subprocess.run(["ffmpeg", "-i", path, "-af", "silencedetect=noise=-35dB:d=0.15", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    starts = [float(x) for x in re.findall(r"silence_start: ([\d.]+)", log)]
    ends = [float(x) for x in re.findall(r"silence_end: ([\d.]+)", log)]
    return [round(e - s, 2) for s, e in zip(starts, ends) if s > 0.05 and e < dur - 0.05]


def transcribe(path, key):
    body = {"contents": [{"parts": [
        {"inline_data": {"mime_type": "audio/mp4", "data": base64.b64encode(open(path, "rb").read()).decode()}},
        {"text": "Transcribe this English speech verbatim. Output only the transcript."}]}],
        "generationConfig": {"temperature": 0}}
    req = urllib.request.Request(
        f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent",
        data=json.dumps(body).encode(), headers={"x-goog-api-key": key, "Content-Type": "application/json"})
    for attempt in range(5):  # 503, 429 는 잠깐 뒤 다시 보낸다
        try:
            resp = json.load(urllib.request.urlopen(req, timeout=120))
            break
        except urllib.error.HTTPError as e:
            if e.code not in (429, 503) or attempt == 4:
                raise
            time.sleep(5 * (attempt + 1))
    return resp["candidates"][0]["content"]["parts"][0]["text"].strip()


def main():
    assert wer("a b c d", "a x c") == 0.5  # 치환 1, 삭제 1
    key = os.environ.get("GEMINI_API_KEY") or sys.exit("GEMINI_API_KEY 가 필요하다")
    questions = [l for l in open(os.path.join(HERE, "questions.txt")).read().splitlines() if l]
    rows = []
    for name in sorted(os.listdir(os.path.join(HERE, "samples"))):
        path = os.path.join(HERE, "samples", name)
        ref = questions[int(re.search(r"q(\d)", name).group(1)) - 1]
        # 받아쓰기가 가끔 끝 문장을 빠뜨린다. 처음 WER 이 기준을 넘을 때만 2번 더 받아쓰고 중앙값을 쓴다
        # (무료 키의 모델별 하루 요청 수가 적어서 모든 샘플을 3번씩 받아쓰지 않는다)
        hyps, dur = [transcribe(path, key)], duration(path)
        if wer(ref, hyps[0]) > THRESHOLD:
            hyps += [transcribe(path, key) for _ in range(2)]
        wers = sorted(round(wer(ref, h), 3) for h in hyps)
        rows.append({"file": name, "wer": wers[len(wers) // 2], "wer_runs": wers, "wpm": round(len(words(ref)) / dur * 60),
                     "pauses_s": inner_pauses(path, dur), "duration_s": round(dur, 2), "transcripts": hyps})
    json.dump({"transcriber": MODEL, "wer_threshold": THRESHOLD, "samples": rows}, sys.stdout, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    main()
