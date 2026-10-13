"""스토어 문구가 두 스토어의 칸 제한 안에 들고, 빠진 칸 없이 있는지 본다.

글자 수를 넘기거나 파일이 빠지면 업로드 명령이 계정 키를 넣은 뒤에야 실패한다. 키 없이 여기서 먼저 잡는다.

실행: python3 -m unittest scripts/test_store_metadata.py
"""

import re
import unittest
from pathlib import Path

FASTLANE = Path(__file__).resolve().parent.parent / "fastlane"
PLAY = FASTLANE / "metadata/android/ko-KR"
IOS = FASTLANE / "metadata/ios/ko"
REVIEW_NOTES = FASTLANE / "metadata/ios/review_information/notes.txt"

# 파일 이름: 최대 글자 수
PLAY_LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}
IOS_LIMITS = {
    "name.txt": 30,
    "subtitle.txt": 30,
    "promotional_text.txt": 170,
    "description.txt": 4000,
    "keywords.txt": 100,
    "support_url.txt": 255,
    "privacy_url.txt": 255,
}


def text(path):
    return path.read_text().strip()


class StoreMetadataTest(unittest.TestCase):
    def assert_within_limits(self, folder, limits):
        for name, limit in limits.items():
            with self.subTest(file=f"{folder.name}/{name}"):
                value = text(folder / name)
                self.assertTrue(value, "빈 칸")
                self.assertLessEqual(len(value), limit)

    def test_play_text_fits_limits(self):
        self.assert_within_limits(PLAY, PLAY_LIMITS)

    def test_app_store_text_fits_limits(self):
        self.assert_within_limits(IOS, IOS_LIMITS)

    def test_urls_are_https(self):
        for name in ("support_url.txt", "privacy_url.txt"):
            self.assertTrue(text(IOS / name).startswith("https://"), name)

    def test_keywords_have_no_spaces_around_commas(self):
        # 키워드 칸은 쉼표로 나눈 100자다. 쉼표 옆 공백도 글자 수를 먹는다.
        self.assertNotRegex(text(IOS / "keywords.txt"), r"\s,|,\s")

    def test_review_notes_leave_a_slot_for_the_review_key(self):
        notes = text(REVIEW_NOTES)
        self.assertEqual(1, notes.count("{{REVIEW_GEMINI_KEY}}"))
        self.assertLessEqual(len(notes), 4000)

    def test_no_real_key_in_store_files(self):
        leaked = [
            str(p.relative_to(FASTLANE))
            for p in FASTLANE.rglob("*")
            if p.is_file() and re.search(r"AIza[0-9A-Za-z_-]{20}|AQ\.[0-9A-Za-z_-]{20}", p.read_text(errors="ignore"))
        ]
        self.assertEqual([], leaked)


if __name__ == "__main__":
    unittest.main()
