"""raw/ 응답을 읽어 result.json 을 만든다. 네트워크를 쓰지 않는다."""
import json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
raw = lambda n: json.load(open(f"{HERE}/raw/{n}.json"))
REF = " ".join(re.findall(r'say -v \S+ -r \d+ -o \S+ "([^"]+)"', open(f"{HERE}/make_audio.sh").read()))
DURATION = 59.12  # ffprobe
TRUE_PAUSES = [(22.03, 23.58), (39.90, 41.96)]  # silencedetect -40dB 0.8s
NUM = {"30": "thirty", "3": "three"}


def words(t):
    return [NUM.get(w, w) for w in re.findall(r"[a-z0-9']+", t.lower())]


def wer(hyp):  # 단어 오류율, 편집 거리
    r, h = words(REF), words(hyp)
    d = list(range(len(h) + 1))
    for i, rw in enumerate(r, 1):
        prev, d[0] = d[0], i
        for j, hw in enumerate(h, 1):
            prev, d[j] = d[j], min(d[j] + 1, d[j - 1] + 1, prev + (rw != hw))
    return round(d[-1] / len(r), 4)


def text(n):
    return raw(n)["candidates"][0]["content"]["parts"][-1]["text"]


def pauses_from_words(ws, min_gap=0.8):
    return [(round(a["e"], 2), round(b["s"], 2)) for a, b in zip(ws, ws[1:]) if b["s"] - a["e"] >= min_gap]


def pause_error(found):
    out = []
    for ts, te in TRUE_PAUSES:
        near = min(found, key=lambda p: abs(p[0] - ts), default=None)
        out.append(None if near is None else {"true": [ts, te], "found": list(near),
                                              "startErrorSec": round(near[0] - ts, 2), "endErrorSec": round(near[1] - te, 2)})
    return {"detected": [list(p) for p in found], "matches": out, "falsePauses": max(0, len(found) - len(TRUE_PAUSES))}


# 단어 시각: 일반 모델(d_words), 받아쓰기 전용(e_transcribe)
gen_words = json.loads(text("d_words"))
tr = raw("e_transcribe")["steps"][0]["content"][0]
sec = lambda s: float(s.rstrip("s"))
tr_words = [{"w": a["text"], "s": sec(a["start_offset"]), "e": sec(a["end_offset"])} for a in tr["annotations"] if a["type"] == "word_info"]

ref_n = len(words(REF))
speak_sec = DURATION - sum(e - s for s, e in TRUE_PAUSES)
struct = json.loads(text("c_struct_ok"))
corr_ok = [c["original"] in struct["transcript"] for c in struct["corrections"]]

# 가격: 100만 토큰당 USD, Standard 등급 (2026-10-07 갱신 가격표)
P = {"gemini-3.8-flash": {"in": 0.75, "out": 3.75, "in2027": 1.50, "out2027": 7.50},
     "gemini-3.5-transcribe": {"in": 2.00, "out": 12.00}}


def usd(model, tin, tout, year="2026"):
    p = P[model]
    i, o = (p["in"], p["out"]) if year == "2026" else (p["in2027"], p["out2027"])
    return round((tin * i + tout * o) / 1e6, 6)


u = raw("c_struct_ok")["usageMetadata"]
ans_in, ans_out = u["promptTokenCount"], u["candidatesTokenCount"] + u.get("thoughtsTokenCount", 0)
cnt = raw("f_session_count")
s3 = raw("f_session3")["usageMetadata"]
audio_per_answer_billed = next(x["tokenCount"] for x in u["promptTokensDetails"] if x["modality"] == "AUDIO")
sess_text = next(x["tokenCount"] for x in cnt["promptTokensDetails"] if x["modality"] == "TEXT")
sess_in_billed = sess_text + 15 * audio_per_answer_billed  # 실제 응답 usage 의 오디오 토큰으로 환산
sess_out_est = ans_out  # 출력+생각 토큰을 답변 1개 분석과 같다고 가정

log = [l.split("\t") for l in open(f"{HERE}/raw/requests.log").read().split("\n") if l]
result = {
    "date": "2026-10-09",
    "models": {"main": "gemini-3.8-flash", "transcribe": "gemini-3.5-transcribe",
               "sessionSendFallback": "gemini-3.5-flash-lite"},
    "audio": {"source": "macOS say 합성 음성(Samantha). 사람 녹음이 아니다", "durationSec": DURATION,
              "pausesInsertedSec": TRUE_PAUSES, "chunks": "5초 m4a(AAC 32kbps) 12개", "full": "m4a 1개 189KB",
              "referenceWords": ref_n, "trueWpmIncludingPauses": round(ref_n / DURATION * 60, 1),
              "trueWpmExcludingPauses": round(ref_n / speak_sec * 60, 1)},
    "requests": {"total": len(log), "byStatus": {s: sum(1 for _, x in log if x == s) for s in sorted({x for _, x in log})},
                 "note": "503 은 모델 과부하(\"high demand\"), 429 는 무료 등급 gemini-3.8-flash 일일 generateContent 20회 한도(응답 메시지 'limit: 20'). 이 실험의 3.8-flash 생성 요청은 15번째에서 막혔다. 같은 키의 다른 호출이 하루 한도를 같이 쓴 것으로 보지만 확인 못했다. countTokens 1회 포함"},
    "1_chunksVsFull": {
        "chunksInline": {"works": True, "wer": wer(text("a_chunks")), "audioTokens": 1502, "seconds": raw("a_chunks")["_spike"]["seconds"]},
        "singleFile": {"works": True, "wer": wer(text("b_full")), "audioTokens": 1479, "seconds": raw("b_full")["_spike"]["seconds"]},
        "note": "조각 경계가 단어 중간을 잘라도 전문은 같았다. 조각 묶음은 마지막 마침표가 빠지고 문단 구분이 없어졌다. 오디오 토큰은 조각 쪽이 23개(1.6%) 많다"},
    "2_audioPlusStructuredOutput": {
        "works": True, "field": "generationConfig.responseFormat.text = {mimeType: \"APPLICATION_JSON\", schema}",
        "pitfall": "mimeType 에 \"application/json\" 을 넣으면 400. 열거값 APPLICATION_JSON 이다",
        "wer": wer(struct["transcript"]), "corrections": len(struct["corrections"]),
        "correctionOriginalInTranscript": corr_ok, "modelWpm": struct["wordsPerMinute"],
        "pauses": pause_error([(p["startSeconds"], p["endSeconds"]) for p in struct["pauses"]]),
        "chunksPlusStructured": "확인 못함. 503 네 번 뒤 429 로 막혔다",
        "thoughtsTokens": u.get("thoughtsTokenCount", 0),
        "noSchemaNote": "gemini-3.5-flash-lite 에 schema 없이 mimeType 만 주니 JSON 이 아닌 마크다운 표를 돌려줬다. schema 를 늘 함께 준다"},
    "3_wordTimestamps": {
        "generalModel": {"model": "gemini-3.8-flash", "words": len(gen_words), "wer": wer(" ".join(w["w"] for w in gen_words)),
                         **pause_error(pauses_from_words(gen_words)), "outputTokens": raw("d_words")["usageMetadata"]["candidatesTokenCount"]},
        "transcribeModel": {"model": "gemini-3.5-transcribe", "endpoint": "POST v1beta/interactions, input type audio inline data 가 된다",
                            "words": len(tr_words), "wer": wer(tr["text"]), **pause_error(pauses_from_words(tr_words)),
                            "outputTokens": raw("e_transcribe")["usage"]["total_output_tokens"]},
        "gapThresholdSec": 0.8},
    "4_cost": {
        "priceUsdPerMTok": P, "billedAudioTokensPerSec": round(audio_per_answer_billed / DURATION, 1),
        "countTokensAudioPerSec": round(next(x["tokenCount"] for x in cnt["promptTokensDetails"] if x["modality"] == "AUDIO") / 15 / DURATION, 1),
        "answer": {"inputTokens": ans_in, "outputPlusThoughtTokens": ans_out,
                   "usd2026": usd("gemini-3.8-flash", ans_in, ans_out), "usd2027": usd("gemini-3.8-flash", ans_in, ans_out, "2027")},
        "sessionGrade": {"assumption": "답변 15개 피드백 JSON + 60초 오디오 15개. 입력은 countTokens 의 텍스트 토큰 + 답변 응답 usage 기준 오디오 토큰x15, 출력은 답변 1개 분석과 같다고 가정",
                         "countTokensTotal": cnt["totalTokens"], "inputTokensBilledEstimate": sess_in_billed, "outputTokensEstimate": sess_out_est,
                         "usd2026": usd("gemini-3.8-flash", sess_in_billed, sess_out_est), "usd2027": usd("gemini-3.8-flash", sess_in_billed, sess_out_est, "2027"),
                         "sentFor3AudiosWith": "gemini-3.5-flash-lite", "sent3Usage": s3},
        "transcribeAnswerUsd": usd("gemini-3.5-transcribe", raw("e_transcribe")["usage"]["total_input_tokens"], raw("e_transcribe")["usage"]["total_output_tokens"]),
        "freeTierBilledUsd": 0},
}
result["conclusions"] = [
    "답변 1개는 녹음 조각을 기기에서 한 m4a 로 이어 붙여 gemini-3.8-flash 한 번 호출(오디오 + responseFormat APPLICATION_JSON 스키마)로 보낸다. 조각을 따로 넣어도 전문은 같았지만 응답이 18.0초로 한 파일(5.6초)보다 느렸다(각 1회 측정)",
    "비용은 생각 토큰이 대부분이다. 답변 1개 약 $0.013 중 출력과 생각 토큰(3,190개)이 90%다. 15문항 회차(답변 15 + 등급 1)는 약 $0.23, 2027년 단가로 $0.46. 생각 예산을 줄였을 때 품질은 다음에 확인한다",
    "무료 등급 gemini-3.8-flash 는 generateContent 하루 20회라 회차 1번(16회)과 재시도면 끝난다. 18번 중 8번이 503 과부하였다. 대기열에 대기 후 재시도와 다른 안정 모델로 바꾸는 경로가 있어야 한다. 멈춤 위치는 일반 모델도 0.45초 안, 받아쓰기 전용 모델은 0.04초 안이었다",
]
c = result["4_cost"]
c["sessionTotal15Answers1Grade"] = {"usd2026": round(15 * c["answer"]["usd2026"] + c["sessionGrade"]["usd2026"], 4),
                                    "usd2027": round(15 * c["answer"]["usd2027"] + c["sessionGrade"]["usd2027"], 4)}
json.dump(result, open(f"{HERE}/result.json", "w"), ensure_ascii=False, indent=1)
print(json.dumps(result, ensure_ascii=False, indent=1))
