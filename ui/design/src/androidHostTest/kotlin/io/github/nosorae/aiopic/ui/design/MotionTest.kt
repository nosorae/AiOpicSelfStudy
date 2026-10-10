package io.github.nosorae.aiopic.ui.design

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 움직임 부품이 움직이는지, 1초 뒤 끝 장면(기준 이미지), 동작 줄이기면 첫 프레임부터 끝 장면인지 본다.
 * 테스트 시계를 멈추고 직접 넘긴다. 라이트와 다크를 나란히 한 장에 그린다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MotionTest {
    @get:Rule val rule = createComposeRule()

    private fun show(reduceMotion: Boolean = false, part: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row {
                listOf(false, true).forEach { dark ->
                    MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) {
                        Column(
                            Modifier.width(180.dp)
                                .background(MontageTheme.colors.backgroundNeutralPrimary)
                                .padding(MontageSpacing.spacing16),
                            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
                        ) { part() }
                    }
                }
            }
        }
    }

    private fun frame(): Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

    private fun settle() = rule.mainClock.advanceTimeBy(1_000)

    /** 처음 보일 때 움직이는 부품: 첫 프레임과 끝이 다르고, 끝 장면을 기준 이미지로 남긴다. */
    private fun assertAppears(part: @Composable () -> Unit) {
        show(part = part)
        val first = frame()
        settle()

        assertFalse("움직이지 않는다", first.sameAs(frame()))
        rule.onRoot().captureRoboImage()
    }

    /** 동작 줄이기면 첫 프레임이 이미 끝 장면이다. */
    private fun assertStill(part: @Composable () -> Unit) {
        show(reduceMotion = true, part = part)
        val first = frame()
        settle()

        assertTrue("동작 줄이기인데 움직인다", first.sameAs(frame()))
    }

    @Composable
    private fun CountUp() = MontageCountUp(77, style = MontageTheme.typography.display1.bold)

    @Composable
    private fun CheckMark() = MontageCheckMark(Modifier.size(48.dp))

    @Test fun countUp() = assertAppears { CountUp() }

    @Test fun countUp_reduceMotion() = assertStill { CountUp() }

    @Test fun checkMark() = assertAppears { CheckMark() }

    @Test fun checkMark_reduceMotion() = assertStill { CheckMark() }

    @Test
    fun progressBar() = fillProgressBar(reduceMotion = false) { start, moving, end ->
        assertFalse("값이 바뀌었는데 그대로다", moving.sameAs(start))
        assertFalse("값이 바뀌면 차오르지 않고 바로 끝 값이다", moving.sameAs(end))
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun progressBar_reduceMotion() = fillProgressBar(reduceMotion = true) { _, moving, end ->
        assertTrue("동작 줄이기인데 차오른다", moving.sameAs(end))
    }

    /** 0.2 로 보인 막대를 0.8 로 바꾸고, 바꾼 직후(400ms 움직임의 50ms 지점)와 1초 뒤를 잡는다. */
    private fun fillProgressBar(reduceMotion: Boolean, check: (Bitmap, Bitmap, Bitmap) -> Unit) {
        var progress by mutableFloatStateOf(0.2f)
        show(reduceMotion) { MontageProgressBar(progress) }
        val start = frame()
        progress = 0.8f
        // 시계를 멈춰 두면 테스트 스레드에서 바꾼 값을 직접 알려야 다음 프레임에 반영된다.
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(50)
        val moving = frame()
        settle()
        check(start, moving, frame())
    }

    /** 파형은 시계로 움직이지 않고 음량을 그대로 그린다. 높이 계산은 공통 테스트가 본다. */
    @Test
    fun waveform() {
        show { MontageWaveform(List(16) { if (it % 2 == 0) 0.9f else 0.4f }) }
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun waveform_reduceMotion() {
        show(reduceMotion = true) { MontageWaveform(List(16) { if (it % 2 == 0) 0.9f else 0.4f }) }
        rule.onRoot().captureRoboImage()
    }
}
