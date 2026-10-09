#!/usr/bin/env python3
"""개발 그래프(docs/graph/dev-graph.json) 검사.

사용: python3 scripts/graph/validate.py [docs/graph/dev-graph.json]
검사: 필수 필드, 상태와 단계 값, done 명령, 안쪽 루프 템플릿(loop)과 횟수 상한, 노드의 상한 덮어쓰기와 attempts,
id 중복, 의존 대상 존재, 노드 사이 순환(dependsOn 만 본다. 안쪽 루프의 되돌아가는 엣지는 상한이 있어 허용한다),
동시에 돌 수 있는 노드끼리 담당 경로 겹침.
다른 그래프 스크립트가 쓰는 load, save, topo_order, loop_limits 도 여기 둔다.
"""
import json
import re
import sys
from fnmatch import fnmatchcase
from pathlib import Path

DEFAULT_PATH = Path(__file__).resolve().parents[2] / "docs" / "graph" / "dev-graph.json"
REQUIRED = ("id", "phase", "kind", "title", "dependsOn", "owns", "reqs", "done", "done_desc", "status")
STATUSES = ("todo", "doing", "done", "blocked")
HUMAN = "human"  # done 명령 대신 쓰는 표시. 사람이 확인해야 끝나는 노드
HANGUL = re.compile("[가-힣]")


def load(path=DEFAULT_PATH):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save(graph, path=DEFAULT_PATH):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(graph, f, ensure_ascii=False, indent=2)
        f.write("\n")


def topo_order(nodes):
    """의존 대상이 먼저 오는 순서. 순환이 있으면 ValueError(순환 경로)."""
    by_id = {n["id"]: n for n in nodes}
    order, state = [], {}

    def visit(nid, stack):
        if state.get(nid) == "done":
            return
        if state.get(nid) == "visiting":
            raise ValueError(" -> ".join(stack[stack.index(nid):] + [nid]))
        state[nid] = "visiting"
        for dep in by_id[nid]["dependsOn"]:
            if dep in by_id:
                visit(dep, stack + [nid])
        state[nid] = "done"
        order.append(by_id[nid])

    for n in nodes:
        visit(n["id"], [])
    return order


def loop_limits(graph, node):
    """되돌아가는 엣지의 출발 단계별 최대 횟수. 노드의 loop 가 템플릿 값을 덮어쓴다. 예: {"done": 3, "review": 2}"""
    limits = {e["from"]: e["max"] for e in graph["loop"]["edges"] if "max" in e}
    limits.update(node.get("loop", {}))
    return limits


def _validate_loop(loop):
    if not isinstance(loop, dict):
        return ["loop: 그래프 최상위에 안쪽 루프 템플릿(steps, edges, exceeded)이 없다. 추가한다"]
    errors = []
    steps = loop.get("steps")
    if not isinstance(steps, list) or not steps or len(set(steps)) != len(steps):
        return ["loop.steps: 겹치지 않는 단계 이름 목록이어야 한다. 예: [\"implement\", \"done\", \"review\", \"merge\"]"]
    for i, e in enumerate(loop.get("edges", [])):
        if e.get("from") not in steps or e.get("to") not in steps:
            errors.append(f"loop.edges[{i}]: from, to 는 steps 의 단계여야 한다")
            continue
        backward = steps.index(e["to"]) <= steps.index(e["from"])
        if "max" in e and (not isinstance(e["max"], int) or isinstance(e["max"], bool) or e["max"] < 1):
            errors.append(f"loop.edges[{i}]: max 는 1 이상의 정수여야 한다")
        elif backward and "max" not in e:
            errors.append(f"loop.edges[{i}]: {e['from']} 에서 {e['to']} 로 되돌아가는 엣지에 횟수 상한 max 가 없다. 추가한다")
    if not loop.get("edges"):
        errors.append("loop.edges: 엣지가 없다")
    if loop.get("exceeded") not in STATUSES:
        errors.append(f"loop.exceeded: 상한을 넘었을 때의 상태가 {', '.join(STATUSES)} 중 하나가 아니다")
    return errors


def _validate_node_loop(name, n, limits):
    errors = []
    done = n["done"]
    if not isinstance(done, str) or not done.strip():
        errors.append(f"{name}: done 은 실행 명령 문자열이거나 \"{HUMAN}\" 이어야 한다")
    elif done != HUMAN and HANGUL.search(done):
        errors.append(f"{name}: done 에 문장이 들어 있다. 실행 명령만 두고 설명은 done_desc 로 옮긴다")
    if not isinstance(n["done_desc"], str) or not n["done_desc"].strip():
        errors.append(f"{name}: done_desc 에 완료 조건 설명을 쓴다")
    for field in ("loop", "attempts"):
        value = n.get(field, {})
        if not isinstance(value, dict):
            errors.append(f"{name}: {field} 는 {{단계: 정수}} 객체여야 한다")
            continue
        for step, count in value.items():
            if step not in limits:
                errors.append(f"{name}: {field} 의 '{step}' 는 횟수 상한이 있는 단계({', '.join(limits)})가 아니다")
            elif not isinstance(count, int) or isinstance(count, bool) or count < (1 if field == "loop" else 0):
                errors.append(f"{name}: {field}.{step} 는 {'1' if field == 'loop' else '0'} 이상의 정수여야 한다")
    return errors


def _paths_overlap(a, b):
    """한쪽이 다른 쪽 안에 들어가면 겹친다. 경로 조각마다 glob(*)을 허용한다."""
    sa, sb = [s for s in a.split("/") if s], [s for s in b.split("/") if s]
    return all(x == y or fnmatchcase(x, y) or fnmatchcase(y, x) for x, y in zip(sa, sb))


def validate(graph):
    errors = []
    nodes = graph.get("nodes", [])
    phases = {p["id"] for p in graph.get("phases", [])}
    errors += _validate_loop(graph.get("loop"))
    if errors:
        return errors
    limits = {e["from"]: e["max"] for e in graph["loop"]["edges"] if "max" in e}

    for i, n in enumerate(nodes):
        name = n.get("id", f"nodes[{i}]")
        missing = [k for k in REQUIRED if k not in n]
        if missing:
            errors.append(f"{name}: 필수 필드 {', '.join(missing)} 가 없다. 노드에 추가한다")
            continue
        if n["status"] not in STATUSES:
            errors.append(f"{name}: status '{n['status']}' 는 허용 값이 아니다. {', '.join(STATUSES)} 중 하나로 바꾼다")
        if n["phase"] not in phases:
            errors.append(f"{name}: phase '{n['phase']}' 가 phases 에 없다. phases 에 추가하거나 기존 단계로 바꾼다")
        if "issue" in n and not isinstance(n["issue"], int):
            errors.append(f"{name}: issue 는 이슈 번호(정수)여야 한다. 예: \"issue\": 12")
        errors += _validate_node_loop(name, n, limits)
    if errors:
        return errors

    ids = [n["id"] for n in nodes]
    for dup in sorted({i for i in ids if ids.count(i) > 1}):
        errors.append(f"{dup}: id 가 중복된다. 한쪽 id 를 바꾼다")
    known = set(ids)
    for n in nodes:
        for dep in n["dependsOn"]:
            if dep not in known:
                errors.append(f"{n['id']}: 의존 대상 '{dep}' 가 그래프에 없다. 노드를 추가하거나 dependsOn 에서 뺀다")
    if errors:
        return errors

    try:
        topo_order(nodes)
    except ValueError as cycle:
        return [f"순환 의존: {cycle}. 이 고리 중 dependsOn 하나를 끊는다"]

    by_id = {n["id"]: n for n in nodes}
    ancestors = {}

    def anc(nid):
        if nid not in ancestors:
            ancestors[nid] = set()
            for dep in by_id[nid]["dependsOn"]:
                ancestors[nid] |= {dep} | anc(dep)
        return ancestors[nid]

    for i, x in enumerate(nodes):
        for y in nodes[i + 1:]:
            if x["id"] in anc(y["id"]) or y["id"] in anc(x["id"]):
                continue  # 순서가 정해진 노드끼리는 같은 경로를 고쳐도 된다
            for px in x["owns"]:
                for py in y["owns"]:
                    if _paths_overlap(px, py):
                        errors.append(
                            f"{x['id']} 와 {y['id']} 가 동시에 돌 수 있는데 담당 경로 '{px}' 와 '{py}' 가 겹친다. "
                            f"경로를 나누거나 한쪽 dependsOn 에 다른 쪽을 넣는다")
    return errors


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PATH
    errors = validate(load(path))
    for e in errors:
        print(f"오류: {e}", file=sys.stderr)
    if errors:
        sys.exit(1)
    print(f"그래프 검사 통과: {path}")


if __name__ == "__main__":
    main()
