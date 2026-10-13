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
APP = FASTLANE.parent / "apps/opic"

STORE_NAME = "말해봄: 영어 스피킹 모의고사"
HOME_NAME = "말해봄"
# 시험 이름은 주관사 상표다. 이름, 부제, 키워드 같은 짧은 칸에 넣으면 심사에서 반려될 수 있다
TRADEMARK = re.compile(r"오픽|OPIc", re.IGNORECASE)
DISCLAIMER = "OPIc 은 ACTFL 의 등록상표이며, 이 앱은 ACTFL 및 시험 주관사와 관련이 없습니다."

# 파일 이름: 최대 글자 수 (BYTE_LIMITED 는 UTF-8 바이트 수)
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
# App Store Connect 키워드 칸은 글자가 아니라 바이트로 잰다. 한글 한 자가 3바이트다
BYTE_LIMITED = {"keywords.txt"}


def text(path):
    return path.read_text().strip()


class StoreMetadataTest(unittest.TestCase):
    def assert_within_limits(self, folder, limits):
        for name, limit in limits.items():
            with self.subTest(file=f"{folder.name}/{name}"):
                value = text(folder / name)
                self.assertTrue(value, "빈 칸")
                size = len(value.encode()) if name in BYTE_LIMITED else len(value)
                self.assertLessEqual(size, limit)

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


    def test_store_name_is_the_same_on_both_stores(self):
        self.assertEqual(STORE_NAME, text(IOS / "name.txt"))
        self.assertEqual(STORE_NAME, text(PLAY / "title.txt"))

    def test_short_fields_leave_out_the_trademark(self):
        fields = [IOS / n for n in ("name.txt", "subtitle.txt", "keywords.txt", "promotional_text.txt")]
        fields += [PLAY / n for n in ("title.txt", "short_description.txt")]
        for path in fields:
            with self.subTest(file=path.name):
                self.assertNotRegex(text(path), TRADEMARK)

    def test_description_names_the_test_once_and_ends_with_the_disclaimer(self):
        for path in (IOS / "description.txt", PLAY / "full_description.txt"):
            with self.subTest(file=path.parent.name):
                value = text(path)
                self.assertTrue(value.endswith(DISCLAIMER))
                body = value.removesuffix(DISCLAIMER)
                self.assertEqual(1, len(TRADEMARK.findall(body)))
                self.assertIn("OPIc 형식", body)

    def test_home_screen_name(self):
        self.assertIn(f'<string name="app_name">{HOME_NAME}</string>', (APP / "androidApp/src/main/res/values/strings.xml").read_text())
        self.assertRegex((APP / "iosApp/iosApp/Info.plist").read_text(), rf"<key>CFBundleDisplayName</key>\s*<string>{HOME_NAME}</string>")


if __name__ == "__main__":
    unittest.main()
