package io.github.nosorae.aiopic.ui.design

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
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
 * 움직임 부품이 움직이는지, 끝 장면(기준 이미지), 동작 줄이기면 첫 프레임부터 멈춘 끝 장면인지 본다.
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

    /** 스프링이 멈추고 뒤따르는 움직임까지 끝날 만큼 넘긴다. */
    private fun settle() = rule.mainClock.advanceTimeBy(2_500)

    /** 테스트 스레드에서 바꾼 값을 알린다. 시계를 멈춰 두면 이걸 해야 다음 프레임에 반영된다. */
    private fun change(block: () -> Unit) {
        block()
        Snapshot.sendApplyNotifications()
    }

    /** 처음 보일 때 한 번 움직이는 부품: 첫 프레임과 끝이 다르고, 끝 장면을 기준 이미지로 남긴다. */
    private fun assertAppears(part: @Composable () -> Unit) {
        show(part = part)
        val first = frame()
        settle()

        assertFalse("움직이지 않는다", first.sameAs(frame()))
        rule.onRoot().captureRoboImage()
    }

    /** 계속 움직이는 부품: 0.3초 사이에 그림이 바뀐다. 1.2초 지점을 기준 이미지로 남긴다. */
    private fun assertKeepsMoving(part: @Composable () -> Unit) {
        show(part = part)
        rule.mainClock.advanceTimeBy(1_200)
        val now = frame()
        rule.onRoot().captureRoboImage()
        rule.mainClock.advanceTimeBy(300)

        assertFalse("움직이지 않는다", now.sameAs(frame()))
    }

    /** 동작 줄이기면 첫 프레임이 이미 끝 장면이고 시간이 가도 그대로다. 그 멈춘 장면을 기준 이미지로 남긴다. */
    private fun assertStill(part: @Composable () -> Unit) {
        show(reduceMotion = true, part = part)
        val first = frame()
        settle()

        assertTrue("동작 줄이기인데 움직인다", first.sameAs(frame()))
        rule.onRoot().captureRoboImage()
    }

    @Composable
    private fun CountUp(key: String? = null) = MontageCountUp(77, style = MontageTheme.typography.display1.bold, playOnceKey = key)

    @Composable
    private fun CheckMark() = MontageCheckMark(Modifier.size(96.dp))

    @Composable
    private fun GradeScale(key: String? = null) = MontageGradeScale(listOf("NL", "NM", "NH", "IL", "IM", "IH", "AL"), low = 4, high = 5, description = "IM~IH", playOnceKey = key)

    @Composable
    private fun VoiceRibbon() = MontageVoiceRibbon(level = 0.7f)

    @Composable
    private fun GatheringDots() = MontageGatheringDots("질문을 불러오는 중")

    @Test fun countUp() = assertAppears { CountUp() }

    @Test fun countUp_reduceMotion() = assertStill { CountUp() }

    @Test fun checkMark() = assertAppears { CheckMark() }

    @Test fun checkMark_reduceMotion() = assertStill { CheckMark() }

    @Test fun gradeScale() = assertAppears { GradeScale() }

    @Test fun gradeScale_reduceMotion() = assertStill { GradeScale() }

    @Test fun voiceRibbon() = assertKeepsMoving { VoiceRibbon() }

    @Test fun voiceRibbon_reduceMotion() = assertStill { VoiceRibbon() }

    @Test fun gatheringDots() = assertKeepsMoving { GatheringDots() }

    @Test fun gatheringDots_reduceMotion() = assertStill { GatheringDots() }

    @Test
    fun `한 번 움직인 등급 눈금은 스크롤로 나갔다 돌아와도 끝 장면 그대로다`() = assertTrue(backFromScroll { GradeScale(key = "s1:IM~IH") })

    @Test
    fun `한 번 굴러간 점수 숫자는 스크롤로 나갔다 돌아와도 끝 값 그대로다`() = assertTrue(backFromScroll { CountUp(key = "a1:77") })

    @Test
    fun `키가 없으면 스크롤로 나갔다 돌아올 때 다시 움직인다`() = assertFalse(backFromScroll { GradeScale() })

    @Test
    fun `한 번 움직인 등급 눈금은 화면을 떠났다 다시 그려도 끝 장면 그대로다`() {
        var visible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme { if (visible) Box(Modifier.width(360.dp).testTag(PART)) { GradeScale(key = "s1:IM~IH") } } }
        settle()
        val end = part()
        change { visible = false }
        rule.mainClock.advanceTimeByFrame()
        change { visible = true }
        rule.mainClock.advanceTimeByFrame()

        assertTrue("다시 그리니 처음부터 움직인다", end.sameAs(part()))
    }

    /**
     * 목록 맨 위 부품이 움직여 멈춘 뒤 스크롤로 화면 밖에 나갔다(그 칸이 버려진다) 돌아온다. 돌아온 첫 프레임이 끝 장면과 같으면 true 다.
     * 회차 리포트와 기록의 등급 구슬, 답변 리포트의 점수가 모두 LazyColumn 칸 안에 있어서 이 경우를 본다.
     */
    private fun backFromScroll(content: @Composable () -> Unit): Boolean {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MontageTheme {
                LazyColumn(Modifier.width(360.dp).height(300.dp).testTag(LIST)) {
                    item { Box(Modifier.fillMaxWidth().testTag(PART)) { content() } }
                    items(30) { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
        settle()
        val end = part()
        rule.onNodeWithTag(LIST).performScrollToIndex(30)
        settle()
        rule.onNodeWithTag(LIST).performScrollToIndex(0)
        rule.mainClock.advanceTimeByFrame()
        return end.sameAs(part())
    }

    private fun part(): Bitmap = rule.onNodeWithTag(PART).captureToImage().asAndroidBitmap()

    @Test
    fun progressBar() = assertFills(reduceMotion = false) { start, moving, end ->
        assertFalse("값이 바뀌었는데 그대로다", moving.sameAs(start))
        assertFalse("값이 바뀌면 차오르지 않고 바로 끝 값이다", moving.sameAs(end))
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun progressBar_reduceMotion() = assertFills(reduceMotion = true) { _, moving, end ->
        assertTrue("동작 줄이기인데 차오른다", moving.sameAs(end))
    }

    @Test
    fun stepTrail() = assertNextStep(reduceMotion = false) { start, moving, end ->
        assertFalse("문항을 끝냈는데 그대로다", moving.sameAs(start))
        assertFalse("선이 뻗지 않고 바로 다음 점이다", moving.sameAs(end))
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun stepTrail_reduceMotion() = assertNextStep(reduceMotion = true) { _, moving, end ->
        assertTrue("동작 줄이기인데 선이 뻗는다", moving.sameAs(end))
    }

    @Test
    fun answerSteps() = assertNextAnswerStep(reduceMotion = false) { start, moving, end ->
        assertFalse("단계가 바뀌었는데 그대로다", moving.sameAs(start))
        assertFalse("다음 단계가 차오르지 않고 바로 끝이다", moving.sameAs(end))
        rule.onRoot().captureRoboImage()
    }

    @Test
    fun answerSteps_reduceMotion() = assertNextAnswerStep(reduceMotion = true) { _, moving, end ->
        assertTrue("동작 줄이기인데 차오른다", moving.sameAs(end))
    }

    @Test fun playButtonPlaying() = assertKeepsMoving { MontagePlayButton(text = "Play", busy = true, onClick = {}) }

    @Test fun playButtonPlaying_reduceMotion() = assertStill { MontagePlayButton(text = "Play", busy = true, onClick = {}) }

    /** 듣기에서 답하기로 넘어간다. 피드백 단계까지 셋을 보인다. */
    private fun assertNextAnswerStep(reduceMotion: Boolean, check: (Bitmap, Bitmap, Bitmap) -> Unit) {
        var step by mutableStateOf(AnswerStep.Listen)
        show(reduceMotion) { MontageAnswerSteps(current = step, withFeedback = true) }
        assertChange({ step = AnswerStep.Answer }, check)
    }

    /** 0.2 로 보인 막대를 0.8 로 바꾸고, 바꾸고 0.2초 뒤(400ms 움직임의 중간쯤)와 끝을 잡는다. */
    private fun assertFills(reduceMotion: Boolean, check: (Bitmap, Bitmap, Bitmap) -> Unit) {
        var progress by mutableFloatStateOf(0.2f)
        show(reduceMotion) { MontageProgressBar(progress) }
        assertChange({ progress = 0.8f }, check)
    }

    /** 15문항 중 3문항을 끝낸 진행 점에서 4번째를 끝낸다. */
    private fun assertNextStep(reduceMotion: Boolean, check: (Bitmap, Bitmap, Bitmap) -> Unit) {
        var done by mutableIntStateOf(3)
        show(reduceMotion) { MontageStepTrail(total = 15, done = done) }
        assertChange({ done = 4 }, check)
    }

    private fun assertChange(block: () -> Unit, check: (Bitmap, Bitmap, Bitmap) -> Unit) {
        val start = frame()
        change(block)
        rule.mainClock.advanceTimeBy(200)
        val moving = frame()
        settle()
        check(start, moving, frame())
    }

    private companion object {
        const val LIST = "list"
        const val PART = "part"
    }
}
