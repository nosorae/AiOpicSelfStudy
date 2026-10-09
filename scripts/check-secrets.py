#!/usr/bin/env python3
"""커밋 범위에서 새로 들어간 줄에 키, 토큰, 개인 경로가 있는지 검사한다. 하나라도 있으면 0 이 아닌 값으로 끝난다.

공개 저장소라 한 번 푸시된 값은 히스토리에서 지워도 남는다. 그래서 범위 전체 diff 가 아니라 커밋마다 본다.
pre-push 훅과 CI 가 같은 검사를 쓴다.

    scripts/check-secrets.py origin/main..HEAD
"""

import re
import subprocess
import sys

# 패턴 문자열이 이 파일 자체에 걸리지 않게 일부 글자를 문자 집합으로 감싼다.
PATTERNS = [
    ("Google API 키", re.compile(r"AIza[0-9A-Za-z_-]{35}")),
    ("GitHub 토큰", re.compile(r"gh[pousr]_[0-9A-Za-z]{30,}")),
    ("API 토큰(sk-)", re.compile(r"sk-[A-Za-z0-9]{20,}")),
    ("iOS 서명 팀 ID", re.compile(r"DEVELOPMENT_TEAM = [A-Z0-9]{10}")),
    ("개인 키", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("맥 개인 절대경로", re.compile(r"/U[s]ers/[^/\s]+/")),
]
LOCAL_FILES = ("local.properties", "Local.xcconfig")
FIX = (
    "고칠 방법: 값을 커밋하지 않는 local.properties(Android, Gradle) 나 Local.xcconfig(iOS) 로 옮기고 코드는 그 값을 읽게 한다. "
    "경로는 프로젝트 기준 상대경로나 환경변수로 바꾼다. 이미 커밋했다면 git rebase -i 로 그 커밋에서 값을 빼고 다시 푸시한다."
)


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def problems_in(commit):
    found = []
    for path in git("show", "--format=", "--name-only", "--diff-filter=A", commit).splitlines():
        if path.rsplit("/", 1)[-1] in LOCAL_FILES:
            found.append(f"{path}: 기기마다 다른 설정 파일을 커밋했다")
    path, line_no = None, 0
    for line in git("show", "--format=", "--unified=0", "--no-color", "--no-ext-diff", commit).splitlines():
        if line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else None
        elif line.startswith("@@"):
            line_no = int(re.search(r"\+(\d+)", line).group(1))
        elif line.startswith("+") and path:
            for label, pattern in PATTERNS:
                if pattern.search(line):
                    found.append(f"{path}:{line_no}: {label} 형태의 값이 들어갔다")
            line_no += 1
    return found


def main():
    if len(sys.argv) != 2:
        sys.exit("사용법: scripts/check-secrets.py <커밋 범위, 예: origin/main..HEAD>")
    failed = False
    for commit in git("rev-list", "--no-merges", sys.argv[1]).split():
        for problem in problems_in(commit):
            print(f"[민감 정보] {git('log', '-1', '--format=%h %s', commit).strip()}\n  {problem}", file=sys.stderr)
            failed = True
    if failed:
        print(FIX, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
