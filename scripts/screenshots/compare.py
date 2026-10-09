#!/usr/bin/env python3
"""UI 가 바뀐 PR 의 스크린샷 전후 비교 표를 만든다. 파이썬 표준 라이브러리와 git 만 쓴다.

기준 브랜치(기본 origin/main)와 HEAD 사이에서 바뀐 Roborazzi 기준 이미지(`**/screenshots/*.png`)를 찾아
화면마다 바꾸기 전과 바꾼 뒤를 나란히 붙인 PNG 를 만든다. 이미지를 고아 브랜치 `pr-assets` 의 `pr-<번호>/` 에
올리고 PR 본문에 붙일 마크다운 표를 출력한다. 작업 트리와 현재 브랜치는 건드리지 않는다.

    ./gradlew recordRoborazziAndroidHostTest   # 바뀐 화면의 기준 이미지를 다시 찍고 커밋한다
    scripts/screenshots/compare.py 12           # PR #12 용 표를 출력한다
    scripts/screenshots/compare.py 12 --dry-run # 올리지 않고 build/screenshot-compare/ 에만 쓴다
"""

import argparse
import os
import re
import struct
import subprocess
import sys
import tempfile
import zlib

ASSET_BRANCH = "pr-assets"
GAP = 24  # 전후 이미지 사이 여백(px)


def git(*args, data=None, env=None):
    return subprocess.run(["git", *args], input=data, env=env, check=True, capture_output=True).stdout


def read_png(data):
    """8비트 RGB, RGBA, 회색조 PNG 를 (너비, 높이, RGBA 행 목록) 으로 읽는다."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "PNG 가 아니다"
    pos, idat = 8, b""
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos : pos + 8])
        body = data[pos + 8 : pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    channels = {0: 1, 2: 3, 4: 2, 6: 4}.get(color)
    if depth != 8 or channels is None or interlace:
        sys.exit(f"지원하지 않는 PNG 형식이다(비트 {depth}, 색 {color}, 인터레이스 {interlace}).")
    raw, stride, rows, prev = zlib.decompress(idat), width * channels, [], bytearray(width * channels)
    for y in range(height):
        start = y * (stride + 1)
        kind, line = raw[start], bytearray(raw[start + 1 : start + 1 + stride])
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            if kind == 1:
                line[i] = (line[i] + a) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + b) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xFF
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xFF
        prev = line
        rgba = bytearray()
        for x in range(width):
            px = line[x * channels : (x + 1) * channels]
            if channels <= 2:
                rgba += bytes([px[0]] * 3) + bytes([px[1] if channels == 2 else 255])
            else:
                rgba += px + (b"" if channels == 4 else b"\xff")
        rows.append(bytes(rgba))
    return width, height, rows


def write_png(width, height, rows):
    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))

    pixels = b"".join(b"\x00" + row for row in rows)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(pixels, 9))
        + chunk(b"IEND", b"")
    )


def side_by_side(before, after):
    (w1, h1, r1), (w2, h2, r2) = read_png(before), read_png(after)
    height, white = max(h1, h2), b"\xff\xff\xff\xff"
    rows = []
    for y in range(height):
        left = r1[y] if y < h1 else white * w1
        right = r2[y] if y < h2 else white * w2
        rows.append(left + white * GAP + right)
    return write_png(w1 + GAP + w2, height, rows)


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
            files[f"{name}_compare.png"] = side_by_side(before, after)
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

    print("| 화면 | 바꾸기 전 | 바꾼 뒤 |")
    print("|---|---|---|")
    for name, modified in screens:
        title = f"[{name}]({url(name + '_compare.png')})" if modified else f"{name} (새 화면)"
        before = f'<img src="{url(name + "_before.png")}" width="300">' if modified else "없음"
        print(f'| {title} | {before} | <img src="{url(name + "_after.png")}" width="300"> |')
    for name in removed:
        print(f"| {name} (지운 화면) | | 없음 |")


if __name__ == "__main__":
    main()
