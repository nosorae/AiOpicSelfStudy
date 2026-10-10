#!/usr/bin/env python3
"""UI 가 바뀐 PR 의 스크린샷 전후 비교 표를 만든다. 파이썬 표준 라이브러리와 git 만 쓴다.

기준 브랜치(기본 origin/main)와 HEAD 사이에서 바뀐 Roborazzi 기준 이미지(`**/screenshots/*.png`)를 찾아
바꾸기 전과 바꾼 뒤 이미지를 고아 브랜치 `pr-assets` 의 `pr-<번호>/` 에 올리고,
PR 본문에 붙일 두 열(바꾸기 전, 바꾼 뒤) 마크다운 표를 출력한다. 작업 트리와 현재 브랜치는 건드리지 않는다.

    ./gradlew recordRoborazziAndroidHostTest   # 바뀐 화면의 기준 이미지를 다시 찍고 커밋한다
    scripts/screenshots/compare.py 12           # PR #12 용 표를 출력한다
    scripts/screenshots/compare.py 12 --dry-run # 올리지 않고 build/screenshot-compare/ 에만 쓴다
"""

import argparse
import os
import re
import subprocess
import tempfile

ASSET_BRANCH = "pr-assets"


def git(*args, data=None, env=None):
    return subprocess.run(["git", *args], input=data, env=env, check=True, capture_output=True).stdout


def changed_screenshots(base):
    out = git("diff", "--name-status", "--no-renames", f"{base}...HEAD", "--", ":(glob)**/screenshots/**.png").decode()
    return [line.split("\t", 1) for line in out.splitlines()]


def upload(pr, files):
    """files(이름: 바이트)를 pr-assets 브랜치의 pr-<번호>/ 에 커밋하고 그 커밋 해시를 돌려준다."""
    subprocess.run(["git", "fetch", "-q", "origin", ASSET_BRANCH], capture_output=True)
    parent = subprocess.run(
        ["git", "rev-parse", "-q", "--verify", f"refs/remotes/origin/{ASSET_BRANCH}"], capture_output=True, text=True
    ).stdout.strip()
    with tempfile.TemporaryDirectory() as tmp:
        env = {**os.environ, "GIT_INDEX_FILE": os.path.join(tmp, "index")}
        if parent:
            git("read-tree", parent, env=env)
            git("rm", "-r", "-q", "--cached", "--ignore-unmatch", f"pr-{pr}", env=env)
        for name, data in files.items():
            blob = git("hash-object", "-w", "--stdin", data=data).decode().strip()
            git("update-index", "--add", "--cacheinfo", f"100644,{blob},pr-{pr}/{name}", env=env)
        tree = git("write-tree", env=env).decode().strip()
    parents = ["-p", parent] if parent else []
    commit = git("commit-tree", tree, *parents, "-m", f"PR #{pr} 스크린샷 전후 비교").decode().strip()
    git("push", "-q", "origin", f"{commit}:refs/heads/{ASSET_BRANCH}")
    return commit


def main():
    parser = argparse.ArgumentParser(description="PR 본문용 스크린샷 전후 비교 표를 만든다.")
    parser.add_argument("pr", type=int, help="PR 번호")
    parser.add_argument("--base", default="origin/main", help="바꾸기 전 기준 (기본 origin/main)")
    parser.add_argument("--dry-run", action="store_true", help="올리지 않고 build/screenshot-compare/ 에 쓴다")
    args = parser.parse_args()
    os.chdir(git("rev-parse", "--show-toplevel").decode().strip())

    files, screens, removed = {}, [], []
    for status, path in changed_screenshots(args.base):
        # 기준 이미지 이름은 `패키지.클래스.메서드.png` 라서 뒤 두 마디만 화면 이름으로 쓴다.
        name = ".".join(os.path.basename(path)[: -len(".png")].split(".")[-2:])
        if status == "D":
            removed.append(name)
            continue
        after = git("show", f"HEAD:{path}")
        files[f"{name}_after.png"] = after
        if status == "M":
            before = git("show", f"{args.base}:{path}")
            files[f"{name}_before.png"] = before
        screens.append((name, status == "M"))

    if not screens and not removed:
        print("UI 변경 없음")
        return

    if args.dry_run:
        out = os.path.join("build", "screenshot-compare", f"pr-{args.pr}")
        os.makedirs(out, exist_ok=True)
        for name, data in files.items():
            with open(os.path.join(out, name), "wb") as f:
                f.write(data)
        url = lambda name: f"{out}/{name}"  # noqa: E731
    else:
        commit = upload(args.pr, files)
        remote = git("remote", "get-url", "origin").decode().strip()
        slug = re.search(r"github\.com[:/](.+?)(?:\.git)?$", remote).group(1)
        url = lambda name: f"https://github.com/{slug}/raw/{commit}/pr-{args.pr}/{name}"  # noqa: E731

    # 화면 이름 열은 두지 않는다. 표가 좁아져 전후 이미지가 크게 보인다. 이름은 이미지에 마우스를 올리면 보인다.
    img = lambda name, tag: f'<img src="{url(name + tag)}" width="300" alt="{name}" title="{name}">'  # noqa: E731
    print("| 바꾸기 전 | 바꾼 뒤 |")
    print("|---|---|")
    for name, modified in screens:
        before = img(name, "_before.png") if modified else "없음 (새 화면)"
        print(f"| {before} | {img(name, '_after.png')} |")
    if removed:
        print(f"\n지운 화면: {', '.join(removed)}")


if __name__ == "__main__":
    main()
