// Montage v4.0.0 (MIT, Copyright (c) Wanted Lab, Inc.) 의 색 XML 을 Kotlin 상수로 옮겼다. 손으로 고치지 않는다.
package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Montage 역할 색 148개와 이 앱이 더한 두 개. 두 테마 값이 다르면 다크 값, 라이트 값 순서로 적는다.
 *
 * brand 가 들어간 역할 색만 Montage 파랑 대신 주홍으로 바꿨다(ADR 0018). 투명도는 Montage 값을 그대로 둔다.
 * 라이트는 `#C83F1A`(눌림 `#B83A18`, `#A43415`), 다크는 `#FF8A62`(눌림 `#EB7F5A`, `#D17150`)다.
 *
 * 생성자 인자로 148개를 받지 않는 것은 JVM 메서드 인자 한도(255칸)를 넘기 때문이다.
 *
 * - `brandDecoration`: 등급 그래프, 진행 막대 같은 장식용 강조색. 라이트 `#E9572B` 는 흰 글자 대비가 4.5:1 에 못 미쳐 글자 배경에 쓰지 않는다.
 * - `onBrand`: brand 표면 위 글자와 아이콘 색. 다크 주홍 위에는 흰 글자 대비가 모자라 검정 계열을 쓴다.
 */
@Immutable
class MontageColors internal constructor(dark: Boolean) {
    val staticWhite = MontagePalette.Common100
    val staticBlack = MontagePalette.Common0
    val foregroundNeutralPrimary = if (dark) MontagePalette.CoolNeutral99 else MontagePalette.CoolNeutral10
    val foregroundNeutralStrong = if (dark) MontagePalette.Common100 else MontagePalette.Common0
    val foregroundNeutralSecondary = if (dark) MontagePalette.CoolNeutral90Opacity88 else MontagePalette.CoolNeutral22Opacity88
    val foregroundNeutralTertiary = if (dark) MontagePalette.CoolNeutral80Opacity61 else MontagePalette.CoolNeutral25Opacity61
    val foregroundNeutralQuaternary = if (dark) MontagePalette.CoolNeutral80Opacity28 else MontagePalette.CoolNeutral25Opacity28
    val foregroundNeutralInverse = if (dark) MontagePalette.CoolNeutral10 else MontagePalette.CoolNeutral99
    val foregroundDisablePrimary = if (dark) MontagePalette.CoolNeutral70Opacity16 else MontagePalette.CoolNeutral25Opacity16
    val foregroundInactivePrimary = if (dark) MontagePalette.CoolNeutral40 else MontagePalette.CoolNeutral70
    val foregroundBrandPrimary = if (dark) Color(0xFFFF8A62) else Color(0xFFC83F1A)
    val foregroundBrandInverse = if (dark) Color(0xFFC83F1A) else Color(0xFFFF8A62)
    val foregroundPositivePrimary = if (dark) MontagePalette.Green60 else MontagePalette.Green50
    val foregroundCautionaryPrimary = if (dark) MontagePalette.Orange60 else MontagePalette.Orange50
    val foregroundNegativePrimary = if (dark) MontagePalette.Red60 else MontagePalette.Red50
    val foregroundNegativeStrong = if (dark) MontagePalette.Red60 else MontagePalette.Red40
    val foregroundAccentLime = if (dark) MontagePalette.Lime50 else MontagePalette.Lime37
    val foregroundAccentCyan = if (dark) MontagePalette.Cyan50 else MontagePalette.Cyan40
    val foregroundAccentLightblue = if (dark) MontagePalette.Lightblue50 else MontagePalette.Lightblue40
    val foregroundAccentViolet = if (dark) MontagePalette.Violet70 else MontagePalette.Violet45
    val foregroundAccentPurple = if (dark) MontagePalette.Purple60 else MontagePalette.Purple40
    val foregroundAccentPink = if (dark) MontagePalette.Pink60 else MontagePalette.Pink46
    val backgroundNeutralPrimary = if (dark) MontagePalette.CoolNeutral15 else MontagePalette.Common100
    val backgroundNeutralSecondary = if (dark) MontagePalette.CoolNeutral5 else MontagePalette.CoolNeutral99
    val surfaceNeutralPrimary = if (dark) MontagePalette.CoolNeutral15 else MontagePalette.Common100
    val surfaceNeutralSecondary = if (dark) MontagePalette.CoolNeutral50Opacity22 else MontagePalette.CoolNeutral50Opacity8
    val surfaceNeutralTertiary = if (dark) MontagePalette.CoolNeutral50Opacity12 else MontagePalette.CoolNeutral50Opacity5
    val surfaceNeutralStrong = if (dark) MontagePalette.CoolNeutral50Opacity28 else MontagePalette.CoolNeutral50Opacity16
    val surfaceNeutralInverse = if (dark) MontagePalette.Common100 else MontagePalette.CoolNeutral15
    val surfaceElevatedPrimary = if (dark) MontagePalette.CoolNeutral17 else MontagePalette.Common100
    val surfaceElevatedSecondary = if (dark) MontagePalette.CoolNeutral7 else MontagePalette.CoolNeutral99
    val surfaceBrandPrimary = if (dark) Color(0xFFFF8A62) else Color(0xFFC83F1A)
    val surfaceBrandStrong = if (dark) Color(0xFFEB7F5A) else Color(0xFFB83A18)
    val surfaceBrandHeavy = if (dark) Color(0xFFD17150) else Color(0xFFA43415)
    val surfaceBrandSubtle = if (dark) Color(0x0DFF8A62) else Color(0x0DC83F1A)
    val surfacePositivePrimary = if (dark) MontagePalette.Green60Opacity8 else MontagePalette.Green50Opacity8
    val surfaceCautionaryPrimary = if (dark) MontagePalette.Orange60Opacity8 else MontagePalette.Orange50Opacity8
    val surfaceNegativePrimary = if (dark) MontagePalette.Red60Opacity8 else MontagePalette.Red50Opacity8
    val surfaceNegativeStrong = if (dark) MontagePalette.Red60Opacity22 else MontagePalette.Red50Opacity12
    val surfaceDisablePrimary = if (dark) MontagePalette.CoolNeutral22 else MontagePalette.CoolNeutral98
    val surfaceAccentLime = if (dark) MontagePalette.Lime60Opacity8 else MontagePalette.Lime50Opacity8
    val surfaceAccentCyan = if (dark) MontagePalette.Cyan60Opacity8 else MontagePalette.Cyan50Opacity8
    val surfaceAccentLightblue = if (dark) MontagePalette.Lightblue60Opacity8 else MontagePalette.Lightblue50Opacity8
    val surfaceAccentViolet = if (dark) MontagePalette.Violet60Opacity8 else MontagePalette.Violet50Opacity8
    val surfaceAccentPurple = if (dark) MontagePalette.Purple60Opacity8 else MontagePalette.Purple50Opacity8
    val surfaceAccentPink = if (dark) MontagePalette.Pink60Opacity8 else MontagePalette.Pink50Opacity8
    val surfaceAccentLimeOpaque = if (dark) MontagePalette.Lime60 else MontagePalette.Lime50
    val surfaceAccentCyanOpaque = if (dark) MontagePalette.Cyan60 else MontagePalette.Cyan50
    val surfaceAccentLightblueOpaque = if (dark) MontagePalette.Lightblue60 else MontagePalette.Lightblue50
    val surfaceAccentVioletOpaque = if (dark) MontagePalette.Violet60 else MontagePalette.Violet50
    val surfaceAccentPurpleOpaque = if (dark) MontagePalette.Purple60 else MontagePalette.Purple50
    val surfaceAccentPinkOpaque = if (dark) MontagePalette.Pink60 else MontagePalette.Pink50
    val lineNeutralPrimary = if (dark) MontagePalette.CoolNeutral50Opacity35 else MontagePalette.CoolNeutral50Opacity22
    val lineNeutralSecondary = if (dark) MontagePalette.CoolNeutral50Opacity28 else MontagePalette.CoolNeutral50Opacity16
    val lineNeutralTertiary = if (dark) MontagePalette.CoolNeutral50Opacity22 else MontagePalette.CoolNeutral50Opacity8
    val lineNeutralPrimaryOpaque = if (dark) MontagePalette.CoolNeutral25 else MontagePalette.CoolNeutral96
    val lineNeutralSecondaryOpaque = if (dark) MontagePalette.CoolNeutral23 else MontagePalette.CoolNeutral97
    val lineNeutralTertiaryOpaque = if (dark) MontagePalette.CoolNeutral22 else MontagePalette.CoolNeutral98
    val lineBrandPrimary = if (dark) Color(0x47FF8A62) else Color(0x47C83F1A)
    val lineBrandStrong = if (dark) Color(0x6EFF8A62) else Color(0x6EC83F1A)
    val lineBrandFocus = if (dark) Color(0x1FFF8A62) else Color(0x1FC83F1A)
    val linePositivePrimary = if (dark) MontagePalette.Green60Opacity43 else MontagePalette.Green50Opacity43
    val lineCautionaryPrimary = if (dark) MontagePalette.Orange60Opacity43 else MontagePalette.Orange50Opacity43
    val lineNegativePrimary = if (dark) MontagePalette.Red60Opacity43 else MontagePalette.Red50Opacity43
    val lineNegativeStrong = if (dark) MontagePalette.Red60Opacity52 else MontagePalette.Red50Opacity52
    val lineNegativeFocus = if (dark) MontagePalette.Red60Opacity12 else MontagePalette.Red50Opacity12
    val lineAccentLime = if (dark) MontagePalette.Lime50Opacity43 else MontagePalette.Lime37Opacity43
    val lineAccentCyan = if (dark) MontagePalette.Cyan50Opacity43 else MontagePalette.Cyan40Opacity43
    val lineAccentLightblue = if (dark) MontagePalette.Lightblue50Opacity43 else MontagePalette.Lightblue40Opacity43
    val lineAccentViolet = if (dark) MontagePalette.Violet70Opacity43 else MontagePalette.Violet45Opacity43
    val lineAccentPurple = if (dark) MontagePalette.Purple60Opacity43 else MontagePalette.Purple40Opacity43
    val lineAccentPink = if (dark) MontagePalette.Pink60Opacity43 else MontagePalette.Pink46Opacity43
    val effectTransparentPrimary = if (dark) MontagePalette.CoolNeutral17Opacity61 else MontagePalette.Common100Opacity8
    val effectTransparentSecondary = if (dark) MontagePalette.CoolNeutral17Opacity61 else MontagePalette.Common100Opacity28
    val effectDimmerPrimary = if (dark) MontagePalette.CoolNeutral10Opacity74 else MontagePalette.CoolNeutral10Opacity52
    val staticWhiteOpacity5 = Color(0x0DFFFFFF)
    val staticWhiteOpacity8 = Color(0x14FFFFFF)
    val staticWhiteOpacity12 = Color(0x1FFFFFFF)
    val staticWhiteOpacity22 = Color(0x38FFFFFF)
    val staticWhiteOpacity28 = Color(0x47FFFFFF)
    val staticWhiteOpacity35 = Color(0x59FFFFFF)
    val staticWhiteOpacity52 = Color(0x85FFFFFF)
    val staticWhiteOpacity61 = Color(0x9CFFFFFF)
    val staticWhiteOpacity74 = Color(0xBDFFFFFF)
    val staticWhiteOpacity88 = Color(0xE0FFFFFF)
    val staticBlackOpacity0 = Color(0x00000000)
    val staticBlackOpacity8 = Color(0x14000000)
    val staticBlackOpacity12 = Color(0x1F000000)
    val staticBlackOpacity22 = Color(0x38000000)
    val staticBlackOpacity28 = Color(0x47000000)
    val staticBlackOpacity35 = Color(0x59000000)
    val staticBlackOpacity43 = Color(0x6E000000)
    val staticBlackOpacity52 = Color(0x85000000)
    val staticBlackOpacity74 = Color(0xBD000000)
    val staticBlackOpacity88 = Color(0xE0000000)
    val surfaceBrandPrimaryOpacity5 = if (dark) Color(0x0DFF8A62) else Color(0x0DC83F1A)
    val surfaceBrandPrimaryOpacity8 = if (dark) Color(0x14FF8A62) else Color(0x14C83F1A)
    val surfaceBrandPrimaryOpacity12 = if (dark) Color(0x1FFF8A62) else Color(0x1FC83F1A)
    val surfaceBrandPrimaryOpacity22 = if (dark) Color(0x38FF8A62) else Color(0x38C83F1A)
    val surfaceBrandPrimaryOpacity28 = if (dark) Color(0x47FF8A62) else Color(0x47C83F1A)
    val surfaceBrandPrimaryOpacity35 = if (dark) Color(0x59FF8A62) else Color(0x59C83F1A)
    val surfaceBrandPrimaryOpacity52 = if (dark) Color(0x85FF8A62) else Color(0x85C83F1A)
    val surfaceBrandPrimaryOpacity61 = if (dark) Color(0x9CFF8A62) else Color(0x9CC83F1A)
    val surfaceBrandPrimaryOpacity74 = if (dark) Color(0xBDFF8A62) else Color(0xBDC83F1A)
    val surfaceBrandPrimaryOpacity88 = if (dark) Color(0xE0FF8A62) else Color(0xE0C83F1A)
    val foregroundNeutralPrimaryOpacity5 = if (dark) Color(0x0DF7F7F8) else Color(0x0D171719)
    val foregroundNeutralPrimaryOpacity8 = if (dark) Color(0x14F7F7F8) else Color(0x14171719)
    val foregroundNeutralPrimaryOpacity12 = if (dark) Color(0x1FF7F7F8) else Color(0x1F171719)
    val foregroundNeutralStrongOpacity5 = if (dark) Color(0x0DFFFFFF) else Color(0x0D000000)
    val foregroundNeutralStrongOpacity8 = if (dark) Color(0x14FFFFFF) else Color(0x14000000)
    val foregroundNeutralStrongOpacity12 = if (dark) Color(0x1FFFFFFF) else Color(0x1F000000)
    val foregroundNeutralStrongOpacity35 = if (dark) Color(0x59FFFFFF) else Color(0x59000000)
    val foregroundNeutralStrongOpacity52 = if (dark) Color(0x85FFFFFF) else Color(0x85000000)
    val foregroundNeutralStrongOpacity74 = if (dark) Color(0xBDFFFFFF) else Color(0xBD000000)
    val foregroundNeutralStrongOpacity88 = if (dark) Color(0xE0FFFFFF) else Color(0xE0000000)
    val foregroundNeutralTertiaryOpacity5 = if (dark) Color(0x0DAEB0B6) else Color(0x0D37383C)
    val foregroundNeutralTertiaryOpacity8 = if (dark) Color(0x14AEB0B6) else Color(0x1437383C)
    val foregroundNeutralTertiaryOpacity12 = if (dark) Color(0x1FAEB0B6) else Color(0x1F37383C)
    val foregroundNeutralTertiaryOpacity35 = if (dark) Color(0x59AEB0B6) else Color(0x5937383C)
    val foregroundNeutralTertiaryOpacity52 = if (dark) Color(0x85AEB0B6) else Color(0x8537383C)
    val foregroundNeutralTertiaryOpacity74 = if (dark) Color(0xBDAEB0B6) else Color(0xBD37383C)
    val foregroundNeutralTertiaryOpacity88 = if (dark) Color(0xE0AEB0B6) else Color(0xE037383C)
    val backgroundNeutralPrimaryOpacity0 = if (dark) Color(0x001B1C1E) else Color(0x00FFFFFF)
    val backgroundNeutralPrimaryOpacity61 = if (dark) Color(0x9C1B1C1E) else Color(0x9CFFFFFF)
    val surfaceElevatedPrimaryOpacity0 = if (dark) Color(0x00212225) else Color(0x00FFFFFF)
    val surfaceElevatedPrimaryOpacity12 = if (dark) Color(0x1F212225) else Color(0x1FFFFFFF)
    val surfaceElevatedPrimaryOpacity88 = if (dark) Color(0xE0212225) else Color(0xE0FFFFFF)
    val surfaceElevatedPrimaryOpacity97 = if (dark) Color(0xF7212225) else Color(0xF7FFFFFF)
    val lineNeutralPrimaryOpaqueOpacity28 = if (dark) Color(0x4737383C) else Color(0x47E1E2E4)
    val lineNeutralPrimaryOpaqueOpacity61 = if (dark) Color(0x9C37383C) else Color(0x9CE1E2E4)
    val lineNeutralTertiaryOpaqueOpacity52 = if (dark) Color(0x852E2F33) else Color(0x85F4F4F5)
    val foregroundPositivePrimaryOpacity5 = if (dark) Color(0x0D1ED45A) else Color(0x0D00BF40)
    val foregroundPositivePrimaryOpacity8 = if (dark) Color(0x141ED45A) else Color(0x1400BF40)
    val foregroundPositivePrimaryOpacity12 = if (dark) Color(0x1F1ED45A) else Color(0x1F00BF40)
    val foregroundPositivePrimaryOpacity16 = if (dark) Color(0x291ED45A) else Color(0x2900BF40)
    val foregroundPositivePrimaryOpacity43 = if (dark) Color(0x6E1ED45A) else Color(0x6E00BF40)
    val foregroundNegativePrimaryOpacity8 = if (dark) Color(0x14FF6363) else Color(0x14FF4242)
    val foregroundNegativePrimaryOpacity12 = if (dark) Color(0x1FFF6363) else Color(0x1FFF4242)
    val surfaceAccentCyanOpaqueOpacity8 = if (dark) Color(0x1428D0ED) else Color(0x1400BDDE)
    val surfaceAccentCyanOpaqueOpacity35 = if (dark) Color(0x5928D0ED) else Color(0x5900BDDE)
    val surfaceAccentLightblueOpaqueOpacity5 = if (dark) Color(0x0D3DC2FF) else Color(0x0D00AEFF)
    val surfaceAccentLightblueOpaqueOpacity8 = if (dark) Color(0x143DC2FF) else Color(0x1400AEFF)
    val surfaceAccentLightblueOpaqueOpacity12 = if (dark) Color(0x1F3DC2FF) else Color(0x1F00AEFF)
    val surfaceAccentVioletOpaqueOpacity5 = if (dark) Color(0x0D7D5EF7) else Color(0x0D6541F2)
    val surfaceAccentVioletOpaqueOpacity8 = if (dark) Color(0x147D5EF7) else Color(0x146541F2)
    val surfaceAccentVioletOpaqueOpacity12 = if (dark) Color(0x1F7D5EF7) else Color(0x1F6541F2)
    val surfaceAccentPinkOpaqueOpacity8 = if (dark) Color(0x14FA73E3) else Color(0x14F553DA)
    val surfaceAccentLimeOpaqueOpacity8 = if (dark) Color(0x146BE016) else Color(0x1458CF04)
    val brandDecoration = if (dark) Color(0xFFFF8A62) else Color(0xFFE9572B)
    val onBrand = if (dark) MontagePalette.CoolNeutral10 else MontagePalette.Common100
}

internal val MontageLightColors = MontageColors(dark = false)

internal val MontageDarkColors = MontageColors(dark = true)
