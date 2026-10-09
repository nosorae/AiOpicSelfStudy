#!/usr/bin/env python3
"""그래프 노드와 GitHub 이슈를 맞춘다. 여러 번 돌려도 결과가 같다.

사용: python3 scripts/graph/sync_issues.py [--dry-run] [--graph 경로] [--repo owner/name]
- issue 가 없는 노드: 이슈를 만들고 번호를 dev-graph.json 에 적는다
- 이슈 제목을 그래프 제목 `[노드id] 제목` 에 맞춘다. 닫힌 이슈도 제목만 고친다
- done: 이슈를 닫는다. done 이 아닌데 닫혀 있으면(PR 병합) 다시 열지 않고 알림만 낸다
- blocked: status:blocked 라벨을 달고 blockedReason 을 댓글로 남긴다. 풀리면 라벨을 뗀다
"""
import argparse
import json
import subprocess
import sys

from validate import DEFAULT_PATH, HUMAN, load, save, topo_order, validate

HUMAN_KINDS = ("human", "gate")
LABEL_COLORS = {"phase": "0E8A16", "by": "C5DEF5", "kind": "BFD4F2", "status": "E11D21"}


def gh(*args, repo=None):
    cmd = ["gh", *args] + (["-R", repo] if repo else [])
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd)} 실패: {result.stderr.strip()}")
    return result.stdout


def labels_for(node):
    by = "human" if node["kind"] in HUMAN_KINDS else "agent"
    return [f"phase:{node['phase']}", f"by:{by}", f"kind:{node['kind']}"]


def issue_body(node, phase_title, by_id):
    todo = [f"- [ ] {node['title']}"]
    if node["reqs"]:
        todo.append(f"- [ ] 요구사항 {', '.join(node['reqs'])} 을 만족한다 (번호는 `docs/specs/product.md` 의 요구사항 번호)")
    if node["done"] == HUMAN:
        todo.append("- [ ] 사람이 완료 조건을 확인한다")
        done = f"{node['done_desc']}\n\n사람이 확인한다."
    else:
        todo.append(f"- [ ] `scripts/graph/run_done.sh {node['id']}` 가 0 으로 끝난다")
        done = f"{node['done_desc']}\n\n```\n{node['done']}\n```"
    owns = "\n".join(f"- `{p}`" for p in node["owns"]) or "-"
    deps = ", ".join(f"#{by_id[d]['issue']}" if "issue" in by_id[d] else f"`{d}`" for d in node["dependsOn"]) or "-"
    return (
        f"## 배경\n{node['phase']} {phase_title} 단계의 작업이다. {node['title']}.\n\n"
        f"## 할 일\n" + "\n".join(todo) + "\n\n"
        f"## 완료 조건\n{done}\n\n"
        f"## 고칠 범위\n{owns}\n\n"
        f"## 먼저 끝날 것\n{deps}\n\n"
        f"## 노드\n`{node['id']}` ({node['phase']})\n"
    )


def sync(graph, path, dry_run=False, repo=None, run=gh):
    def write(*args):
        print(("[dry-run] gh " if dry_run else "gh ") + " ".join(args))
        return "" if dry_run else run(*args, repo=repo)

    phases = {p["id"]: p["title"] for p in graph["phases"]}
    by_id = {n["id"]: n for n in graph["nodes"]}
    existing_labels = {l["name"] for l in json.loads(run("label", "list", "--limit", "200", "--json", "name", repo=repo))}
    issues = {i["number"]: i for i in json.loads(
        run("issue", "list", "--state", "all", "--limit", "500", "--json", "number,state,title,labels", repo=repo))}

    def ensure_label(name):
        if name not in existing_labels:
            write("label", "create", name, "--color", LABEL_COLORS[name.split(":")[0]])
            existing_labels.add(name)

    for node in topo_order(graph["nodes"]):
        if "issue" not in node:
            labels = labels_for(node)
            for name in labels:
                ensure_label(name)
            body = issue_body(node, phases[node["phase"]], by_id)
            title = f"[{node['id']}] {node['title']}"
            args = ["issue", "create", "--title", title, "--body", body] + [a for l in labels for a in ("--label", l)]
            if dry_run:
                print(f"[dry-run] 이슈 생성: {title} 라벨 {', '.join(labels)}")
                continue
            url = run(*args, repo=repo).strip()
            node["issue"] = int(url.rstrip("/").rsplit("/", 1)[1])
            issues[node["issue"]] = {"number": node["issue"], "state": "OPEN", "title": title, "labels": []}
            save(graph, path)  # 중간에 실패해도 만든 번호는 남긴다
            print(f"이슈 생성: #{node['issue']} {title}")

        issue = issues.get(node["issue"])
        if issue is None:
            print(f"경고: {node['id']} 의 이슈 #{node['issue']} 를 찾지 못했다", file=sys.stderr)
            continue
        num = str(node["issue"])
        title = f"[{node['id']}] {node['title']}"
        if issue.get("title") != title:
            write("issue", "edit", num, "--title", title)
        if node["status"] == "done" and issue["state"] == "OPEN":
            write("issue", "close", num)
        elif node["status"] != "done" and issue["state"] == "CLOSED":
            # PR 병합이 이슈를 먼저 닫는다. 다시 열지 않고 그래프를 맞추라고 알린다
            print(f"알림: #{num} 은 닫혔는데 {node['id']} 는 {node['status']} 이다. "
                  f"끝났으면 scripts/graph/set_status.sh {node['id']} done 을 돌린다", file=sys.stderr)
        blocked_label = "status:blocked" in {l["name"] for l in issue["labels"]}
        if node["status"] == "blocked" and not blocked_label:
            ensure_label("status:blocked")
            write("issue", "edit", num, "--add-label", "status:blocked")
            write("issue", "comment", num, "--body", f"막힘: {node.get('blockedReason', '이유 미기재')}")
        elif node["status"] != "blocked" and blocked_label:
            write("issue", "edit", num, "--remove-label", "status:blocked")


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--dry-run", action="store_true", help="GitHub 를 바꾸지 않고 할 일만 출력한다")
    ap.add_argument("--graph", default=DEFAULT_PATH)
    ap.add_argument("--repo", help="owner/name. 없으면 현재 git 원격을 쓴다")
    a = ap.parse_args()
    graph = load(a.graph)
    errors = validate(graph)
    if errors:
        sys.exit("그래프 검사 실패. 먼저 validate.py 로 고친다:\n" + "\n".join(errors))
    try:
        sync(graph, a.graph, a.dry_run, a.repo)
    except RuntimeError as e:
        sys.exit(f"이슈 동기화 실패: {e}")


if __name__ == "__main__":
    main()
