#!/usr/bin/env python3
"""dev-graph.json 을 mermaid 그림(docs/graph/dev-graph.md)으로 만든다.

사용: python3 scripts/graph/render.py [그래프 경로] [출력 경로]
"""
import sys
from pathlib import Path

from validate import DEFAULT_PATH, load

COLORS = {  # 상태별 채움색, 테두리색
    "todo": ("#eeeeee", "#999999"),
    "doing": ("#fff3b0", "#d4a017"),
    "done": ("#c8f7c5", "#2e7d32"),
    "blocked": ("#ffcdd2", "#c62828"),
}


def mid(node_id):
    return node_id.replace("-", "_")


LOOP_LABELS = {"implement": "구현", "done": "done 명령", "review": "리뷰", "merge": "병합"}


def render_loop(loop):
    """노드마다 같은 안쪽 루프를 한 번만 그린다."""
    lines = ["flowchart LR"]
    for step in loop["steps"]:
        lines.append(f'  L_{step}["{LOOP_LABELS.get(step, step)}"]')
    lines.append(f'  L_{loop["exceeded"]}(["{loop["exceeded"]}: 사람"])')
    for e in loop["edges"]:
        label = e.get("on", "")
        if "max" in e:
            label += f" 최대 {e['max']}번"
            lines.append(f"  L_{e['from']} -. 넘으면 .-> L_{loop['exceeded']}")
        arrow = f"-->|{label}|" if label else "-->"
        lines.append(f"  L_{e['from']} {arrow} L_{e['to']}")
    return "\n".join(lines)


def render(graph):
    lines = ["flowchart TD"]
    for phase in graph["phases"]:
        lines.append(f'  subgraph {phase["id"]}["{phase["id"]} {phase["title"]}"]')
        for n in graph["nodes"]:
            if n["phase"] == phase["id"]:
                issue = f' #{n["issue"]}' if "issue" in n else ""
                title = n["title"].replace('"', "'")
                lines.append(f'    {mid(n["id"])}["{n["id"]}{issue}<br/>{title}"]')
        lines.append("  end")
    for n in graph["nodes"]:
        for dep in n["dependsOn"]:
            lines.append(f"  {mid(dep)} --> {mid(n['id'])}")
    for status, (fill, stroke) in COLORS.items():
        lines.append(f"  classDef {status} fill:{fill},stroke:{stroke},color:#000")
        members = [mid(n["id"]) for n in graph["nodes"] if n["status"] == status]
        if members:
            lines.append(f"  class {','.join(members)} {status}")

    counts = {s: sum(n["status"] == s for n in graph["nodes"]) for s in COLORS}
    legend = ", ".join(f"{s} {c}" for s, c in counts.items())
    return (
        "# 개발 그래프\n\n"
        "`scripts/graph/render.py` 가 `dev-graph.json` 에서 만든다. 손으로 고치지 않는다.\n\n"
        f"노드 {len(graph['nodes'])}개: {legend}. 색은 회색 todo, 노랑 doing, 초록 done, 빨강 blocked 이다.\n\n"
        "```mermaid\n" + "\n".join(lines) + "\n```\n\n"
        "## 노드 안쪽 루프\n\n"
        "모든 노드가 같은 반복을 돈다. `dev-graph.json` 의 `loop` 가 원본이고 노드의 `loop` 가 횟수 상한을 덮어쓴다. "
        "done 이 `human` 인 노드는 사람이 확인한다.\n\n"
        "```mermaid\n" + render_loop(graph["loop"]) + "\n```\n"
    )


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_PATH
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else src.with_suffix(".md")
    out.write_text(render(load(src)), encoding="utf-8")
    print(f"그림 생성: {out}")


if __name__ == "__main__":
    main()
