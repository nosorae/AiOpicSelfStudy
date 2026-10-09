#!/usr/bin/env bash
# 노드 상태를 바꾸고 이슈, 프로젝트를 뒤에서 맞춘다.
# 사용: scripts/graph/set_status.sh <노드id> <todo|doing|done|blocked> [이유]
# doing 은 시작일, done 은 종료일을 오늘로 적는다. blocked 는 이유를 blockedReason 에 적는다.
# 동기화 실패는 로그에만 남기고 개발을 막지 않는다. 로그: ${GRAPH_SYNC_LOG:-${TMPDIR:-/tmp}/aiopic-graph-sync.log}
set -euo pipefail
dir="$(cd "$(dirname "$0")" && pwd)"
[ $# -ge 2 ] || { echo "사용: $0 <노드id> <todo|doing|done|blocked> [이유]" >&2; exit 64; }

python3 - "$1" "$2" "${3:-}" <<PY
import sys
from datetime import date
sys.path.insert(0, "$dir")
from validate import STATUSES, load, save, validate

nid, status, reason = sys.argv[1:4]
if status not in STATUSES:
    sys.exit(f"상태는 {', '.join(STATUSES)} 중 하나다: {status}")
graph = load()
node = next((n for n in graph["nodes"] if n["id"] == nid), None)
if node is None:
    sys.exit(f"노드 '{nid}' 가 그래프에 없다")
today = date.today().isoformat()
node["status"] = status
if status in ("doing", "done", "blocked"):
    node.setdefault("start", today)
if status == "done":
    node["end"] = today
else:
    node.pop("end", None)
if status == "todo":
    node.pop("start", None)
if status == "blocked":
    node["blockedReason"] = reason or "이유 미기재"
else:
    node.pop("blockedReason", None)
errors = validate(graph)
if errors:
    sys.exit("\n".join(errors))
save(graph)
print(f"{nid}: {status}")
PY

python3 "$dir/render.py" >/dev/null
log="${GRAPH_SYNC_LOG:-${TMPDIR:-/tmp}/aiopic-graph-sync.log}"
(
  echo "== $(date '+%F %T') $1 $2"
  python3 "$dir/sync_issues.py" || echo "이슈 동기화 실패(다음 갱신 때 맞춘다)"
  python3 "$dir/sync_project.py" || echo "프로젝트 동기화 실패(다음 갱신 때 맞춘다)"
) >>"$log" 2>&1 &
echo "이슈, 프로젝트 동기화를 뒤에서 돌린다. 로그: $log"
