"""app-icon.svg 에서 플랫폼별 PNG 아이콘을 만든다. 벡터 드로어블(ic_launcher_*.xml)은 손으로 옮겨 두고
scripts/test_store_assets.py 가 원본과 같은 호인지 본다.

그림: OPIc 의 O. 오른쪽 위가 열려 있고 그 틈으로 소리 물결이 퍼진다. 색은 ui/design 의 주홍 토큰이다.

실행(저장소 루트에서): python3 apps/opic/icon/make_icon.py
필요: rsvg-convert(librsvg), Pillow
"""

import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent.parent
RES = ROOT / "apps/opic/androidApp/src/main/res"
APP_ICON_SET = ROOT / "apps/opic/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
PLAY_ICON = ROOT / "fastlane/metadata/android/ko-KR/images/icon.png"

LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
NEUTRAL, DARK_PRIMARY = "#171719", "#FF8A62"
BACKGROUND = '<rect width="1024" height="1024" fill="url(#g)"/>'


def render(svg_text, size=1024):
    with tempfile.TemporaryDirectory() as tmp:
        src, out = Path(tmp, "icon.svg"), Path(tmp, "icon.png")
        src.write_text(svg_text)
        subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), str(src), "-o", str(out)], check=True)
        return Image.open(out).convert("RGBA")


def masked(image, size, circle):
    image = image.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if circle:
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.18), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(image, (0, 0), mask)
    return out


def main():
    source = (HERE / "app-icon.svg").read_text()
    assert BACKGROUND in source, "원본 SVG 의 배경 사각형이 바뀌었다. 다크와 틴트 변형을 다시 맞춘다"
    light = render(source)
    # 다크: 어두운 바탕에 다크 테마 주홍 글자. 틴트: 시스템이 색을 입히는 흑백 그림이라 검은 바탕에 흰 글자
    dark = render(source.replace(BACKGROUND, f'<rect width="1024" height="1024" fill="{NEUTRAL}"/>').replace('stroke="#fff"', f'stroke="{DARK_PRIMARY}"'))
    tinted = render(source.replace(BACKGROUND, '<rect width="1024" height="1024" fill="#000"/>')).convert("L")

    light.convert("RGB").save(APP_ICON_SET / "app-icon-1024.png")
    dark.convert("RGB").save(APP_ICON_SET / "app-icon-1024-dark.png")
    tinted.save(APP_ICON_SET / "app-icon-1024-tinted.png")

    PLAY_ICON.parent.mkdir(parents=True, exist_ok=True)
    light.resize((512, 512), Image.LANCZOS).convert("RGB").save(PLAY_ICON)

    # ponytail: minSdk 28 이라 적응형 아이콘만 쓰인다. 레거시 PNG 는 적응형을 못 읽는 런처를 위한 것이다
    for density, size in LEGACY_SIZES.items():
        masked(light, size, circle=False).save(RES / f"mipmap-{density}/ic_launcher.png")
        masked(light, size, circle=True).save(RES / f"mipmap-{density}/ic_launcher_round.png")


if __name__ == "__main__":
    main()
