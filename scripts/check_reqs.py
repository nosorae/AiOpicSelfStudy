#!/usr/bin/env python3
"""요구사항 수락 기준 번호마다 그 번호를 단 테스트가 있는지 본다.

docs/specs/product.md 의 `- F1.0 ...` 줄에서 번호를 읽고, 테스트 소스에서 같은 번호를
찾는다. 테스트 이름이든 주석이든 파일 안에 번호가 있으면 덮인 것으로 본다.
없는 번호를 출력하고 종료 코드 1 로 끝난다.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

TEST_GLOBS = [
    "engine/*/src/*Test/**/*",
    "libs/*/src/*Test/**/*",
    "packs/*/src/*Test/**/*",
    "ui/*/src/*Test/**/*",
    "apps/opic/shared/src/*Test/**/*",
    "apps/opic/androidApp/src/androidTest/**/*",
    "apps/opic/iosApp/iosAppUITests/**/*",
    "tools/eval/src/test/**/*",
]

# 첫 버전 범위 밖이거나 앱 코드로 확인할 대상이 아닌 번호와 그 이유.
EXEMPT = {}

# 테스트 이름에는 점을 못 쓰는 플랫폼이 있어 F5_2 처럼 밑줄로도 적는다.
ID = re.compile(r"F(\d+)[._](\d+)(?!\d)")


def spec_ids():
    text = (ROOT / "docs/specs/product.md").read_text(encoding="utf-8")
    return re.findall(r"^- (F\d+\.\d+) ", text, re.MULTILINE)


def tested_ids():
    found = set()
    for pattern in TEST_GLOBS:
        for path in ROOT.glob(pattern):
            if path.is_file() and path.suffix in {".kt", ".swift"}:
                found.update(f"F{a}.{b}" for a, b in ID.findall(path.read_text(encoding="utf-8")))
    return found


def main():
    ids = spec_ids()
    tested = tested_ids()
    missing = [i for i in ids if i not in tested and i not in EXEMPT]
    for i, why in EXEMPT.items():
        print(f"예외 {i}: {why}")
    for i in missing:
        print(f"테스트 없음 {i}")
    print(f"요구사항 {len(ids)}개, 테스트 있음 {len(ids) - len(missing) - len(EXEMPT)}개, 예외 {len(EXEMPT)}개, 없음 {len(missing)}개")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
