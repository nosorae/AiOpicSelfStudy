#!/usr/bin/env bash
# 노드의 done 명령을 저장소 루트에서 돌려 종료 코드로 판정한다.
# 사용: scripts/graph/run_done.sh <노드id>            done 명령 실행. 실패하면 attempts.done 을 1 올린다
#       scripts/graph/run_done.sh <노드id> review     리뷰 반려 한 번을 attempts.review 에 기록한다
# 횟수가 loop 상한(노드 loop 가 템플릿을 덮어씀)을 넘으면 set_status.sh 로 blocked 로 바꾸고 사람을 부른다.
# 종료 코드: done 명령의 종료 코드. done 이 "human" 이면 2. 리뷰 기록은 상한 안이면 0, 넘으면 1.
set -uo pipefail
dir="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$dir/../.." && pwd)"
[ $# -ge 1 ] || { echo "사용: $0 <노드id> [review]" >&2; exit 64; }
nid="$1"
step="${2:-done}"

cmd="$(python3 - "$nid" <<PY
import sys
sys.path.insert(0, "$dir")
from validate import load
node = next((n for n in load()["nodes"] if n["id"] == sys.argv[1]), None)
if node is None:
    sys.exit(f"노드 '{sys.argv[1]}' 가 그래프에 없다")
print(node["done"])
PY
)" || exit 64

if [ "$step" = done ]; then
  if [ "$cmd" = human ]; then
    echo "$nid: 사람이 확인하는 노드다. 완료 조건은 그래프의 done_desc 를 본다" >&2
    exit 2
  fi
  echo "== $nid done: $cmd"
  (cd "$root" && bash -c "$cmd")
  code=$?
  if [ $code -eq 0 ]; then
    echo "$nid: done 통과"
    exit 0
  fi
elif [ "$step" = review ]; then
  code=1
else
  echo "단계는 done 또는 review 다: $step" >&2
  exit 64
fi

# 실패 한 번을 기록하고 상한을 넘었으면 이유를 출력한다
reason="$(python3 - "$nid" "$step" <<PY
import sys
sys.path.insert(0, "$dir")
from validate import load, loop_limits, save
nid, step = sys.argv[1:3]
graph = load()
node = next(n for n in graph["nodes"] if n["id"] == nid)
attempts = node.setdefault("attempts", {})
attempts[step] = attempts.get(step, 0) + 1
save(graph)
limit = loop_limits(graph, node)[step]
print(f"{nid}: {step} 실패 {attempts[step]}번째, 상한 {limit}번", file=sys.stderr)
if attempts[step] > limit:
    print(f"{step} 실패가 상한 {limit}번을 넘었다")
PY
)"
if [ -n "$reason" ]; then
  "$dir/set_status.sh" "$nid" blocked "$reason"
  exit $(( code == 0 ? 1 : code ))
fi
[ "$step" = review ] && exit 0
exit $code
