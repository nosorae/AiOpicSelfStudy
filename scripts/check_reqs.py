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
EXEMPT = {
    "F8.4": "작업 절차라 테스트 대신 문서로 확인한다 (docs/error-analysis.md 사례 표에 날짜로 시작하는 줄)",
}

# 예외로 둔 번호도 문서 쪽 근거가 있어야 한다. 근거가 없으면 테스트 없음으로 센다.
DOC_CHECKS = {
    "F8.4": lambda: re.search(r"^\| 20\d\d-", (ROOT / "docs/error-analysis.md").read_text(encoding="utf-8"), re.MULTILINE),
}

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
    exempt = {i: why for i, why in EXEMPT.items() if i not in DOC_CHECKS or DOC_CHECKS[i]()}
    missing = [i for i in ids if i not in tested and i not in exempt]
    for i, why in exempt.items():
        print(f"예외 {i}: {why}")
    for i in missing:
        print(f"테스트 없음 {i}")
    print(f"요구사항 {len(ids)}개, 테스트 있음 {len(ids) - len(missing) - len(exempt)}개, 예외 {len(exempt)}개, 없음 {len(missing)}개")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
