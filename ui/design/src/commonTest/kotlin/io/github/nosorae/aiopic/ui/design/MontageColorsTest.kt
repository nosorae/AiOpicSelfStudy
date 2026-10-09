package io.github.nosorae.aiopic.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ADR 0018 의 강조색 표와 대비 기준을 색 상수로 다시 잰다. */
class MontageColorsTest {
    private val light = MontageLightColors
    private val dark = MontageDarkColors

    @Test
    fun brand_colors_are_our_orange_not_montage_blue() {
        assertEquals(Color(0xFFC83F1A), light.surfaceBrandPrimary)
        assertEquals(Color(0xFFC83F1A), light.foregroundBrandPrimary)
        assertEquals(Color(0xFFE9572B), light.brandDecoration)
        assertEquals(Color(0xFFFF8A62), dark.surfaceBrandPrimary)
        assertEquals(Color(0xFFFF8A62), dark.foregroundBrandPrimary)
        assertEquals(Color(0xFFFF8A62), dark.brandDecoration)
    }

    @Test
    fun non_brand_colors_keep_montage_values() {
        assertEquals(Color.White, light.backgroundNeutralPrimary)
        assertEquals(Color(0xFF171719), light.foregroundNeutralPrimary)
        assertEquals(Color(0xFF1B1C1E), dark.backgroundNeutralPrimary)
        assertEquals(Color(0xFFF7F7F8), dark.foregroundNeutralPrimary)
        assertEquals(MontagePalette.Red50, light.foregroundNegativePrimary)
    }

    @Test
    fun brand_opacity_keeps_montage_alpha() {
        // Montage 는 brand 5% 를 0x0D 로 둔다. 색만 바꾸고 투명도는 그대로다.
        assertEquals(Color(0x0DC83F1A), light.surfaceBrandSubtle)
        assertEquals(Color(0x1FFF8A62), dark.lineBrandFocus)
    }

    @Test
    fun light_text_on_brand_meets_body_contrast() {
        assertAtLeast(5.0, light.onBrand, light.surfaceBrandPrimary)
        assertAtLeast(4.5, light.onBrand, light.surfaceBrandStrong)
        assertAtLeast(4.5, light.onBrand, light.surfaceBrandHeavy)
        assertAtLeast(4.5, light.foregroundBrandPrimary, light.backgroundNeutralPrimary)
    }

    @Test
    fun light_decoration_is_graphic_contrast_only() {
        val ratio = contrast(Color.White, light.brandDecoration)
        assertTrue(ratio in 3.0..4.5, "장식색 흰 글자 대비 $ratio 는 3:1 이상 4.5:1 미만이어야 한다")
    }

    @Test
    fun dark_brand_on_measured_dark_background() {
        assertAtLeast(7.0, dark.foregroundBrandPrimary, dark.backgroundNeutralPrimary)
        assertAtLeast(4.5, dark.foregroundBrandPrimary, dark.surfaceElevatedPrimary)
        assertAtLeast(4.5, dark.onBrand, dark.surfaceBrandPrimary)
        assertAtLeast(4.5, dark.onBrand, dark.surfaceBrandStrong)
        assertAtLeast(4.5, dark.onBrand, dark.surfaceBrandHeavy)
    }

    @Test
    fun white_text_fails_on_dark_brand_so_on_brand_is_dark() {
        assertTrue(contrast(Color.White, dark.surfaceBrandPrimary) < 4.5)
    }

    private fun assertAtLeast(min: Double, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue(ratio >= min, "대비 $ratio 가 $min 보다 낮다 (글자 $fg, 배경 $bg)")
    }

    private fun contrast(fg: Color, bg: Color): Double {
        val a = fg.compositeOver(bg).luminance() + 0.05
        val b = bg.luminance() + 0.05
        return max(a, b) / min(a, b)
    }
}
