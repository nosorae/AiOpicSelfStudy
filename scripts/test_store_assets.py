"""앱 아이콘과 스토어 스크린샷이 두 스토어 규격에 맞고, 아이콘이 원본 SVG 와 같은 그림인지 본다.

크기가 틀리거나 파일이 빠지면 업로드나 심사 단계에서야 거절된다. 계정 키 없이 여기서 먼저 잡는다.

실행: python3 -m unittest scripts/test_store_assets.py
"""

import json
import struct
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE_SVG = ROOT / "apps/opic/icon/app-icon.svg"
RES = ROOT / "apps/opic/androidApp/src/main/res"
APP_ICON_SET = ROOT / "apps/opic/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
PLAY_IMAGES = ROOT / "fastlane/metadata/android/ko-KR/images"
APP_STORE_SHOTS = ROOT / "fastlane/screenshots/ios/ko"

ANDROID = "{http://schemas.android.com/apk/res/android}"
SVG = "{http://www.w3.org/2000/svg}"
IPHONE_69 = (1320, 2868)


def png_info(path):
    """(가로, 세로, 알파 채널 여부). PNG 머리(IHDR)만 읽는다."""
    head = path.read_bytes()[:26]
    assert head[:8] == b"\x89PNG\r\n\x1a\n", f"{path.name} 는 PNG 가 아니다"
    width, height = struct.unpack(">II", head[16:24])
    return width, height, head[25] in (4, 6)


def path_data(svg_or_vector, attr):
    return [el.get(attr) for el in ET.parse(svg_or_vector).iter() if el.get(attr)]


def screenshots(folder):
    return sorted(folder.glob("*.png"))


class AppIconTest(unittest.TestCase):
    def test_adaptive_icons_have_foreground_background_and_monochrome(self):
        for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
            with self.subTest(name):
                layers = {el.tag: el.get(f"{ANDROID}drawable") for el in ET.parse(RES / "mipmap-anydpi" / name).getroot()}
                self.assertEqual(
                    {
                        "background": "@drawable/ic_launcher_background",
                        "foreground": "@drawable/ic_launcher_foreground",
                        "monochrome": "@drawable/ic_launcher_monochrome",
                    },
                    layers,
                )

    def test_vector_glyph_is_the_source_svg(self):
        # 벡터 드로어블은 원본 SVG 의 호를 그대로 옮긴다. 원본을 고치고 벡터를 안 고치면 여기서 걸린다.
        source = path_data(SOURCE_SVG, "d")
        self.assertEqual(3, len(source))
        for name in ("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml"):
            with self.subTest(name):
                self.assertEqual(source, path_data(RES / "drawable" / name, f"{ANDROID}pathData"))

    def test_ios_icon_has_light_dark_and_tinted_1024(self):
        images = json.loads((APP_ICON_SET / "Contents.json").read_text())["images"]
        looks = {(img.get("appearances") or [{"value": "light"}])[0]["value"]: img.get("filename") for img in images}
        self.assertEqual({"light", "dark", "tinted"}, set(looks))
        for look, filename in looks.items():
            with self.subTest(look):
                self.assertTrue(filename, "파일 이름이 없다")
                width, height, alpha = png_info(APP_ICON_SET / filename)
                self.assertEqual((1024, 1024), (width, height))
                if look == "light":
                    # App Store 는 알파 채널이 있는 기본 아이콘을 거절한다
                    self.assertFalse(alpha)

    def test_play_store_icon_is_512(self):
        self.assertEqual((512, 512), png_info(PLAY_IMAGES / "icon.png")[:2])


class StoreScreenshotTest(unittest.TestCase):
    def test_play_phone_screenshots(self):
        shots = screenshots(PLAY_IMAGES / "phoneScreenshots")
        self.assertTrue(4 <= len(shots) <= 8, f"{len(shots)}장")
        for shot in shots:
            with self.subTest(shot.name):
                width, height, _ = png_info(shot)
                self.assertTrue(320 <= min(width, height) and max(width, height) <= 3840)
                self.assertLessEqual(max(width, height), 2 * min(width, height), "긴 변이 짧은 변의 두 배를 넘는다")

    def test_app_store_screenshots_are_iphone_69(self):
        shots = screenshots(APP_STORE_SHOTS)
        self.assertTrue(4 <= len(shots) <= 10, f"{len(shots)}장")
        for shot in shots:
            with self.subTest(shot.name):
                self.assertEqual(IPHONE_69, png_info(shot)[:2])


if __name__ == "__main__":
    unittest.main()
