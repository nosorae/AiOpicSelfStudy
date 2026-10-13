"""App Store 업로드가 손 안 대고 끝나게 하는 설정이 남아 있는지 본다.

실행: python3 -m unittest scripts/test_store_upload.py
"""

import plistlib
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FASTFILE = (ROOT / "fastlane/Fastfile").read_text()


class StoreUploadTest(unittest.TestCase):
    def test_info_plist_declares_no_non_exempt_encryption(self):
        # HTTPS 만 쓴다. 이 값이 없으면 빌드마다 App Store Connect 가 수출 규정 질문으로 멈춘다
        info = plistlib.loads((ROOT / "apps/opic/iosApp/iosApp/Info.plist").read_bytes())
        self.assertIs(info.get("ITSAppUsesNonExemptEncryption"), False)

    def test_fastfile_paths_are_absolute(self):
        # fastlane 은 lane 본문을 fastlane/ 에서, 액션을 그 위 폴더에서 돌린다.
        # 상대 경로("./screenshots/ios")를 넘기면 deliver 가 빈 폴더로 읽고 스크린샷 0장을 "성공"으로 끝낸다
        self.assertRegex(FASTFILE, r"(?m)^DIR = File\.expand_path\(")

    def test_build_upload_lane_exists(self):
        self.assertRegex(FASTFILE, r"(?m)^lane :ios_build do")
        self.assertIn("upload_to_testflight(", FASTFILE)


if __name__ == "__main__":
    unittest.main()
