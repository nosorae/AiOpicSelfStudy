"""fastlane/data-safety.csv 가 Play Console 데이터 보안 가져오기 양식 그대로이고 답이 store-declarations.md 와 같은지 본다.

양식은 Play Console 이 내보내는 CSV 의 열과 행(질문 ID, 응답 ID 쌍 763개)을 그대로 쓴다. 행을 빼거나 열 이름을 바꾸면
가져오기가 실패하거나 빈 답으로 덮어쓴다. 답이 있는 칸은 아래 ANSWERS 뿐이어야 한다.

실행: python3 -m unittest scripts/test_data_safety.py
"""

import csv
import unittest
from pathlib import Path

CSV = Path(__file__).resolve().parent.parent / "fastlane/data-safety.csv"
HEADER = [
    "Question ID (machine readable)",
    "Response ID (machine readable)",
    "Response value",
    "Answer requirement",
    "Human-friendly question label",
]
AUDIO = "PSL_DATA_USAGE_RESPONSES:PSL_AUDIO:"

# store-declarations.md 1절과 같은 답. 공유 두 줄(ONLY_SHARED, SHARING_PURPOSE)은 #156 결정 전 초안이다.
ANSWERS = {
    ("PSL_DATA_COLLECTION_COLLECTS_PERSONAL_DATA", ""): "TRUE",
    ("PSL_DATA_COLLECTION_ENCRYPTED_IN_TRANSIT", ""): "TRUE",
    ("PSL_DATA_COLLECTION_USER_REQUEST_DELETE", ""): "FALSE",
    ("PSL_DATA_TYPES_AUDIO", "PSL_AUDIO"): "TRUE",
    (AUDIO + "PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_COLLECTED"): "TRUE",
    (AUDIO + "PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_SHARED"): "TRUE",
    (AUDIO + "PSL_DATA_USAGE_EPHEMERAL", ""): "FALSE",
    (AUDIO + "DATA_USAGE_USER_CONTROL", "PSL_DATA_USAGE_USER_CONTROL_REQUIRED"): "TRUE",
    (AUDIO + "DATA_USAGE_COLLECTION_PURPOSE", "PSL_APP_FUNCTIONALITY"): "TRUE",
    (AUDIO + "DATA_USAGE_SHARING_PURPOSE", "PSL_APP_FUNCTIONALITY"): "TRUE",
}


class DataSafetyCsvTest(unittest.TestCase):
    def setUp(self):
        with CSV.open(newline="", encoding="utf-8") as f:
            self.header, *self.rows = list(csv.reader(f))

    def test_keeps_console_template_shape(self):
        self.assertEqual(HEADER, self.header)
        self.assertEqual(763, len(self.rows))
        self.assertEqual(763, len({(r[0], r[1]) for r in self.rows}))
        self.assertTrue(all(len(r) == 5 for r in self.rows))

    def test_answers_match_store_declarations(self):
        answered = {(r[0], r[1]): r[2] for r in self.rows if r[2]}
        self.assertEqual(ANSWERS, answered)


if __name__ == "__main__":
    unittest.main()
