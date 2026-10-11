"""문서가 가리키는 저장소 안 파일이 실제로 있는지 본다.

코드가 옮겨지거나 지워져도 문서 링크는 그대로 남아 어긋난다. ADR 의 근거 링크, README, architecture 스킬의
상대 링크와 architecture 스킬이 백틱으로 적은 저장소 경로를 확인한다.

실행: python3 -m unittest scripts/test_docs.py
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ARCHITECTURE = ROOT / ".claude/skills/architecture/SKILL.md"
DOCS = [ROOT / "README.md", ARCHITECTURE, *sorted((ROOT / "docs").glob("**/*.md"))]


class DocLinksTest(unittest.TestCase):
    def test_relative_links_point_to_existing_files(self):
        broken = []
        for doc in DOCS:
            for target in re.findall(r"\]\(([^)#\s]+)(?:#[^)]*)?\)", doc.read_text()):
                if "://" not in target and not (doc.parent / target).exists():
                    broken.append(f"{doc.relative_to(ROOT)}: {target}")
        self.assertEqual([], broken)

    def test_architecture_paths_exist(self):
        paths = re.findall(r"`((?:apps|ui|engine|libs|packs|docs)/[^`*<>\s]+)`", ARCHITECTURE.read_text())
        self.assertTrue(paths)
        self.assertEqual([], [p for p in paths if not (ROOT / p).exists()])


if __name__ == "__main__":
    unittest.main()
