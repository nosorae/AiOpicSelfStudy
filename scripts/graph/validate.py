#!/usr/bin/env python3
"""개발 그래프(docs/graph/dev-graph.json) 검사.

사용: python3 scripts/graph/validate.py [docs/graph/dev-graph.json]
검사: 필수 필드, 상태와 단계 값, id 중복, 의존 대상 존재, 순환, 동시에 돌 수 있는 노드끼리 담당 경로 겹침.
다른 그래프 스크립트가 쓰는 load, save, topo_order 도 여기 둔다.
"""
import json
import sys
from fnmatch import fnmatchcase
from pathlib import Path

DEFAULT_PATH = Path(__file__).resolve().parents[2] / "docs" / "graph" / "dev-graph.json"
REQUIRED = ("id", "phase", "kind", "title", "dependsOn", "owns", "reqs", "done", "status")
STATUSES = ("todo", "doing", "done", "blocked")


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


def _paths_overlap(a, b):
    """한쪽이 다른 쪽 안에 들어가면 겹친다. 경로 조각마다 glob(*)을 허용한다."""
    sa, sb = [s for s in a.split("/") if s], [s for s in b.split("/") if s]
    return all(x == y or fnmatchcase(x, y) or fnmatchcase(y, x) for x, y in zip(sa, sb))


def validate(graph):
    errors = []
    nodes = graph.get("nodes", [])
    phases = {p["id"] for p in graph.get("phases", [])}

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
