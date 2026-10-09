#!/usr/bin/env python3
"""그래프 노드 이슈를 GitHub 프로젝트(v2) "AiOpicSelfStudy 개발" 에 맞춘다. 여러 번 돌려도 결과가 같다.

사용: python3 scripts/graph/sync_project.py [--base-date YYYY-MM-DD] [--graph 경로] [--repo owner/name]
- 프로젝트, 필드(상태, 단계, 종류, 노드 id, 시작일, 종료일), 로드맵 화면이 없으면 만든다
- 노드 이슈를 항목으로 넣고 필드 값을 채운다. 시작 전 노드는 의존 순서와 크기 어림으로 예정 날짜를 잡는다
- gh 토큰에 project 권한이 없으면 안내를 출력하고 종료 코드 2 로 끝난다
"""
import argparse
import json
import sys
from datetime import date, timedelta

from sync_issues import gh
from validate import DEFAULT_PATH, load, topo_order, validate

TITLE = "AiOpicSelfStudy 개발"
VIEW = "로드맵"
STATUS_NAMES = {"todo": "대기", "doing": "진행", "done": "완료", "blocked": "막힘"}
# 노드 종류별 작업 일수 어림. ponytail: 주말과 동시 작업 수 제한은 무시한다, 일정이 어긋나면 여기를 고친다
SIZE_DAYS = {"human": 1, "gate": 1, "check": 1, "harness": 1, "spike": 2, "contract": 2, "docs": 2,
             "common": 3, "ai": 3, "platform": 3, "ui": 3}
EXIT_NO_SCOPE = 2


class MissingScope(Exception):
    pass


def plan_dates(graph, base):
    """노드마다 (시작일, 종료일). 기록된 날짜를 먼저 쓰고, 없으면 의존이 끝난 다음 날 시작한다."""
    plan = {}
    for n in topo_order(graph["nodes"]):
        days = SIZE_DAYS.get(n["kind"], 2)
        earliest = max([base] + [plan[d][1] + timedelta(days=1) for d in n["dependsOn"]])
        start = date.fromisoformat(n["start"]) if "start" in n else earliest
        if "end" in n:
            end = date.fromisoformat(n["end"])
        else:
            end = start + timedelta(days=days - 1)
            if n["status"] in ("doing", "blocked"):
                end = max(end, base)
        plan[n["id"]] = (start, end)
    return plan


def call(run, *args):
    try:
        return run(*args)
    except RuntimeError as e:
        if "scope" in str(e).lower() and "project" in str(e).lower():
            raise MissingScope(str(e))
        raise


def sync(graph, owner, repo, base, run=gh):
    def j(*args):
        return json.loads(call(run, *args))

    projects = j("project", "list", "--owner", owner, "--format", "json", "--limit", "100")["projects"]
    project = next((p for p in projects if p["title"] == TITLE), None)
    if project is None:
        project = j("project", "create", "--owner", owner, "--title", TITLE, "--format", "json")
        print(f"프로젝트 생성: #{project['number']} {TITLE}")
    num, pid = str(project["number"]), project["id"]
    try:
        call(run, "project", "link", num, "--owner", owner, "--repo", repo)
    except RuntimeError:
        pass  # 이미 연결돼 있으면 실패한다

    kinds = sorted({n["kind"] for n in graph["nodes"]})
    wanted = {
        "상태": ("SINGLE_SELECT", list(STATUS_NAMES.values())),
        "단계": ("SINGLE_SELECT", [p["id"] for p in graph["phases"]]),
        "종류": ("SINGLE_SELECT", kinds),
        "노드 id": ("TEXT", None),
        "시작일": ("DATE", None),
        "종료일": ("DATE", None),
    }
    fields = {f["name"]: f for f in j("project", "field-list", num, "--owner", owner, "--format", "json", "--limit", "100")["fields"]}
    created = False
    for name, (kind, options) in wanted.items():
        if name not in fields:
            args = ["project", "field-create", num, "--owner", owner, "--name", name, "--data-type", kind, "--format", "json"]
            if options:
                args += ["--single-select-options", ",".join(options)]
            j(*args)
            created = True
            print(f"필드 생성: {name}")
    if created:  # 선택지 id 를 얻으려고 다시 읽는다
        fields = {f["name"]: f for f in j("project", "field-list", num, "--owner", owner, "--format", "json", "--limit", "100")["fields"]}

    ensure_roadmap_view(run, pid)

    plan = plan_dates(graph, base)
    for n in graph["nodes"]:
        if "issue" not in n:
            print(f"건너뜀: {n['id']} 는 이슈가 없다. sync_issues.py 를 먼저 돌린다", file=sys.stderr)
            continue
        url = f"https://github.com/{repo}/issues/{n['issue']}"
        item = j("project", "item-add", num, "--owner", owner, "--url", url, "--format", "json")["id"]
        start, end = plan[n["id"]]
        values = {"상태": STATUS_NAMES[n["status"]], "단계": n["phase"], "종류": n["kind"],
                  "노드 id": n["id"], "시작일": start.isoformat(), "종료일": end.isoformat()}
        for name, value in values.items():
            field = fields[name]
            args = ["project", "item-edit", "--id", item, "--project-id", pid, "--field-id", field["id"]]
            if name in ("상태", "단계", "종류"):
                option = next((o["id"] for o in field.get("options", []) if o["name"] == value), None)
                if option is None:
                    print(f"경고: {name} 필드에 '{value}' 선택지가 없다. 프로젝트에서 선택지를 추가한다", file=sys.stderr)
                    continue
                args += ["--single-select-option-id", option]
            elif name == "노드 id":
                args += ["--text", value]
            else:
                args += ["--date", value]
            call(run, *args)
        print(f"항목 갱신: {n['id']} #{n['issue']} {values['상태']} {values['시작일']}~{values['종료일']}")
    print(f"프로젝트 #{num} 동기화 끝. 열기: gh project view {num} --owner {owner} --web")


def ensure_roadmap_view(run, pid):
    q = "query($id:ID!){node(id:$id){... on ProjectV2{views(first:50){nodes{name layout}}}}}"
    views = json.loads(call(run, "api", "graphql", "-f", f"query={q}", "-f", f"id={pid}"))
    if any(v["name"] == VIEW for v in views["data"]["node"]["views"]["nodes"]):
        return
    m = ("mutation($id:ID!,$name:String!){createProjectV2View(input:{projectId:$id,name:$name,layout:ROADMAP_LAYOUT})"
         "{projectV2View{id}}}")
    try:
        call(run, "api", "graphql", "-f", f"query={m}", "-f", f"id={pid}", "-f", f"name={VIEW}")
        print(f"로드맵 화면 생성: {VIEW}")
    except RuntimeError as e:
        print(f"로드맵 화면을 API 로 만들지 못했다({e}). 프로젝트에서 New view > Roadmap 을 고르고 이름을 '{VIEW}' 로 한다",
              file=sys.stderr)
    print(f"로드맵 화면 '{VIEW}' 의 날짜 필드는 API 로 정할 수 없다. 화면 설정에서 시작 날짜를 '시작일', "
          f"끝 날짜를 '종료일' 로 고르고, 묶음 기준을 '단계' 로 한다")


def main(argv=None, run=gh):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--base-date", type=date.fromisoformat, default=date.today(), help="예정 날짜 기준일, 기본 오늘")
    ap.add_argument("--graph", default=DEFAULT_PATH)
    ap.add_argument("--repo", help="owner/name. 없으면 현재 git 원격을 쓴다")
    a = ap.parse_args(argv)
    graph = load(a.graph)
    errors = validate(graph)
    if errors:
        print("그래프 검사 실패. 먼저 validate.py 로 고친다:\n" + "\n".join(errors), file=sys.stderr)
        return 1
    try:
        repo = a.repo or json.loads(run("repo", "view", "--json", "nameWithOwner"))["nameWithOwner"]
        sync(graph, repo.split("/")[0], repo, a.base_date, run)
    except MissingScope as e:
        print(f"gh 토큰에 project 권한이 없다: {e}\n"
              "다음을 실행해 권한을 더한 뒤 다시 돌린다:\n  gh auth refresh -s project", file=sys.stderr)
        return EXIT_NO_SCOPE
    except RuntimeError as e:
        print(f"프로젝트 동기화 실패: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
