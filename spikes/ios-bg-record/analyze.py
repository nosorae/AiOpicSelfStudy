"""log.jsonl 을 읽어 잠금 중 녹음이 이어졌는지 요약한 JSON 을 찍는다. 사용: python3 analyze.py <log.jsonl>"""
import json, sys
from datetime import datetime

rows = [json.loads(l) for l in open(sys.argv[1]) if l.strip()]
ts = lambda r: datetime.fromisoformat(r["t"].replace("Z", "+00:00"))
chunks = [r for r in rows if r["event"] == "chunk"]
# 잠금은 여러 번 있을 수 있다. 잠금과 바로 다음 해제를 짝지어 가장 긴 구간을 본 측정으로 삼는다.
locks = []
for r in rows:
    if r["event"] == "protectedDataWillBecomeUnavailable":
        locks.append([ts(r), None])
    elif r["event"] == "protectedDataDidBecomeAvailable" and locks and locks[-1][1] is None:
        locks[-1][1] = ts(r)
locks = [l for l in locks if l[1]]
lock, unlock = max(locks, key=lambda l: l[1] - l[0], default=(None, None))
in_lock = [c for c in chunks if lock and lock <= ts(c) <= (unlock or ts(c))]
gaps = [(ts(b) - ts(a)).total_seconds() for a, b in zip(chunks, chunks[1:])]
locked_sec = (unlock - lock).total_seconds() if lock and unlock else None
recorded = sum(c["duration"] for c in in_lock)
print(json.dumps({
    "chunks": len(chunks),
    "lockCount": len(locks),
    "lockedAt": lock and lock.isoformat(), "unlockedAt": unlock and unlock.isoformat(),
    "lockedSeconds": locked_sec,
    "chunksWhileLocked": len(in_lock),
    "recordedSecondsWhileLocked": round(recorded, 1),
    "coverage": round(recorded / locked_sec, 3) if locked_sec else None,
    "maxGapSeconds": max(gaps, default=None),
    "interruptions": [r for r in rows if r["event"] == "interruption"],
    "errors": [r for r in rows if r["event"] in ("recordFailed", "recorderError", "sessionError")],
}, indent=2, ensure_ascii=False))
