package io.github.nosorae.aiopic.ui.featurereport

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * 회차 리포트와 답변 리포트의 상태마다 한 장. 등급 범위와 모델 섞임, 시간 부족, 평가 불가, 발음 평가 안 함은
 * 다크와 글자 200% 도 찍는다. 교정에 "틀림"을 누른 답변(F8.1)도 다크와 글자 200% 를 찍는다. 입력은 미리보기와 같은 [ReportSamples] 다.
 *
 * 테스트 시계를 멈추고 2.5초 넘긴 끝 장면을 찍는다(등급 구슬, 점수 숫자). 끝없이 도는 분석 대기 점은 첫 프레임을 찍는다.
 * 동작 줄이기면 첫 프레임이 그 끝 장면과 같은지도 본다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ReportScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun session_gradeRange_mixedModels() = session(ReportSamples.mixedModels)

    @Test fun session_gradeRange_mixedModels_dark() = session(ReportSamples.mixedModels, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun session_gradeRange_mixedModels_fontScale200() = session(ReportSamples.mixedModels)

    @Config(qualifiers = "w360dp-h1000dp-xhdpi")
    @Test
    fun session_timeShort() = session(ReportSamples.timeUp)

    @Config(qualifiers = "w360dp-h1000dp-xhdpi")
    @Test
    fun session_abandoned() = shot(dark = false, settleMillis = 0) { SessionReport(ReportSamples.abandoned) }

    @Test fun session_abandonedEmpty() = session(ReportSamples.abandonedEmpty)

    @Test
    fun `223 답한 문항 없이 끝난 미완료 회차는 기다림 대신 등급이 없다고 쓰고 움직이지 않는다`() {
        show { SessionReport(ReportSamples.abandonedEmpty) }
        rule.onNodeWithText("답한 문항 없이 끝난 회차라 예상 등급이 없습니다.").assertExists()
        rule.onNodeWithText("미완료").assertExists()
        rule.onNodeWithText("분석했습니다", substring = true).assertDoesNotExist()
        rule.onNodeWithText("예상 등급을 내고 있습니다", substring = true).assertDoesNotExist()
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertTrue("기다릴 것이 없는데 움직인다", first.sameAs(frame()))
    }

    @Config(qualifiers = "w360dp-h1000dp-xhdpi")
    @Test
    fun `223 미완료 회차는 미완료 표시와 답하지 않은 문항을 보인다`() {
        show { SessionReport(ReportSamples.abandoned) }
        rule.onNodeWithText("미완료").assertExists()
        rule.onAllNodesWithText("답하지 않음", substring = true).assertCountEquals(4)
        rule.onNodeWithText("시간 부족", substring = true).assertDoesNotExist()
    }

    @Test fun session_grading() = shot(dark = false, settleMillis = 0) { SessionReport(ReportSamples.grading) }

    @Test
    fun `분석이 다 끝나고 등급만 기다리면 등급을 내는 중이라고 쓰고 점이 계속 움직인다`() {
        show { SessionReport(ReportSamples.grading) }
        rule.onNodeWithText("답변 분석이 끝났습니다. 예상 등급을 내고 있습니다.").assertExists()
        rule.onNodeWithText("답변 7개 중 7개를", substring = true).assertDoesNotExist()
        rule.mainClock.advanceTimeBy(1_200)
        val now = frame()
        rule.mainClock.advanceTimeBy(300)
        assertFalse("등급을 기다리는데 멈춰 있다", now.sameAs(frame()))
    }

    @Test fun answer_evaluated() = answer(ReportSamples.answerEvaluated)

    @Test fun answer_evaluated_dark() = answer(ReportSamples.answerEvaluated, dark = true)

    @Config(qualifiers = "w360dp-h1400dp-xhdpi", fontScale = 2f)
    @Test
    fun answer_evaluated_fontScale200() = answer(ReportSamples.answerEvaluated)

    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun answer_pronunciationNotEvaluated() = answer(ReportSamples.pronunciationNotEvaluated)

    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    @Test
    fun answer_pronunciationNotEvaluated_dark() = answer(ReportSamples.pronunciationNotEvaluated, dark = true)

    @Test fun answer_unscorable() = answer(ReportSamples.answerUnscorable)

    @Config(fontScale = 2f)
    @Test
    fun answer_unscorable_fontScale200() = answer(ReportSamples.answerUnscorable)

    @Test fun answer_silent() = answer(ReportSamples.answerSilent)

    @Test fun answer_silent_dark() = answer(ReportSamples.answerSilent, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun answer_silent_fontScale200() = answer(ReportSamples.answerSilent)

    @Config(qualifiers = "w360dp-h2000dp-xhdpi")
    @Test
    fun answer_scripted() = answer(ReportSamples.answerScripted)

    @Config(qualifiers = "w360dp-h2000dp-xhdpi")
    @Test
    fun answer_scripted_dark() = answer(ReportSamples.answerScripted, dark = true)

    @Config(qualifiers = "w360dp-h4200dp-xhdpi", fontScale = 2f)
    @Test
    fun answer_scripted_fontScale200() = answer(ReportSamples.answerScripted)

    @Config(qualifiers = "w360dp-h2000dp-xhdpi")
    @Test
    fun `217 새 결과는 표현 교정과 따로 내용 교정과 최종 스크립트를 보인다`() {
        show { AnswerReport(ReportSamples.answerScripted) }
        rule.onNodeWithText("내용 교정").assertExists()
        rule.onNodeWithText("공원에서 무엇을 했는지만", substring = true).assertExists()
        rule.onNodeWithTag("final-script").assertExists()
    }

    @Config(qualifiers = "w360dp-h2000dp-xhdpi")
    @Test
    fun `217 최종 스크립트가 없는 옛 결과는 내용 교정과 최종 스크립트 칸을 숨긴다`() {
        show { AnswerReport(ReportSamples.answerEvaluated) }
        rule.onNodeWithText("내용 교정").assertDoesNotExist()
        rule.onNodeWithTag("final-script").assertDoesNotExist()
    }

    @Test fun answer_pending() = answer(ReportSamples.answerPending, settleMillis = 0)

    @Test fun answer_failed() = answer(ReportSamples.answerFailed)

    @Test fun answer_failed_dark() = answer(ReportSamples.answerFailed, dark = true)

    @Config(fontScale = 2f)
    @Test
    fun answer_failed_fontScale200() = answer(ReportSamples.answerFailed)

    @Config(qualifiers = "w360dp-h1000dp-xhdpi")
    @Test
    fun session_analysisFailed() = session(ReportSamples.analysisFailed)

    @Test
    fun `분석하지 못한 답변이 있으면 기다림 대신 실패 수와 다시 분석하기를 보인다`() {
        show { SessionReport(ReportSamples.analysisFailed) }
        rule.onNodeWithText("답변 1개를 분석하지 못했습니다", substring = true).assertExists()
        rule.onNodeWithText("다시 분석하기").assertExists()
        rule.onNodeWithText("예상 등급을 내고 있습니다", substring = true).assertDoesNotExist()
    }

    @Test fun session_gradeFailed() = session(ReportSamples.gradeFailed)

    @Test
    fun `등급을 키 때문에 받지 못했으면 키를 고치라는 이유와 다시 분석하기를 보인다`() {
        show { SessionReport(ReportSamples.gradeFailed) }
        rule.onNodeWithText("예상 등급을 내지 못했습니다. Gemini 키가 맞지 않습니다", substring = true).assertExists()
        rule.onNodeWithText("다시 분석하기").assertExists()
    }

    @Test fun answer_waitingNetwork() = answer(ReportSamples.answerWaitingNetwork, settleMillis = 0)

    @Test fun answer_waitingNetwork_dark() = answer(ReportSamples.answerWaitingNetwork, dark = true, settleMillis = 0)

    @Config(fontScale = 2f)
    @Test
    fun answer_waitingNetwork_fontScale200() = answer(ReportSamples.answerWaitingNetwork, settleMillis = 0)

    @Test fun session_waiting() = shot(dark = false, settleMillis = 0) { SessionReport(ReportSamples.sessionWaiting) }

    @Test fun session_waiting_dark() = shot(dark = true, settleMillis = 0) { SessionReport(ReportSamples.sessionWaiting) }

    @Config(fontScale = 2f)
    @Test
    fun session_waiting_fontScale200() = shot(dark = false, settleMillis = 0) { SessionReport(ReportSamples.sessionWaiting) }

    @Test
    fun `205 기다리는 답변마다 보내는 중인지 연결을 기다리는지 보인다`() {
        show { SessionReport(ReportSamples.sessionWaiting) }
        rule.onNodeWithText("분석하고 있습니다", substring = true).assertExists()
        rule.onNodeWithText("인터넷 연결을 기다립니다", substring = true).assertExists()
    }

    @Test
    fun `205 등급 작업이 연결을 기다리면 등급을 내는 중 대신 연결을 기다린다고 쓴다`() {
        show { SessionReport(ReportSamples.gradeWaitingNetwork) }
        rule.onAllNodesWithText("인터넷 연결을 기다립니다", substring = true).onFirst().assertExists()
        rule.onNodeWithText("예상 등급을 내고 있습니다", substring = true).assertDoesNotExist()
    }

    @Test
    fun `205 다시 보낼 예정이면 남은 초를 보인다`() {
        val at = Clock.System.now() + 90.seconds
        show { AnswerReport(ReportSamples.answerWaitingNetwork.copy(job = ReportSamples.answerWaitingNetwork.job!!.copy(state = JobState.RetryScheduled, nextAttemptAt = at))) }
        rule.onNodeWithText("초 뒤 다시 보냅니다", substring = true).assertExists()
    }

    @Test
    fun `등급 구슬이 눈금 위로 미끄러져 멈춘다`() = assertMoves { SessionReport(ReportSamples.mixedModels) }

    @Test
    fun `동작 줄이기면 등급 구슬이 처음부터 등급 범위에 있다`() = assertStill { SessionReport(ReportSamples.mixedModels) }

    @Test
    fun `점수 숫자가 굴러 올라가 멈춘다`() = assertMoves { AnswerReport(ReportSamples.answerEvaluated) }

    @Test
    fun `동작 줄이기면 점수 숫자가 처음부터 끝 값이다`() = assertStill { AnswerReport(ReportSamples.answerEvaluated) }

    @Test
    fun `동작 줄이기면 분석 대기 점이 모인 채 멈춘다`() = assertStill { AnswerReport(ReportSamples.answerPending) }

    @Composable
    private fun SessionReport(state: SessionReportUiState) = SessionReportScreen(state = state, onOpenAnswer = {}, onIntent = {})

    @Composable
    private fun AnswerReport(state: AnswerReportUiState) = AnswerReportScreen(state = state, onIntent = {})

    private fun session(state: SessionReportUiState, dark: Boolean = false) = shot(dark) { SessionReport(state) }

    private fun answer(state: AnswerReportUiState, dark: Boolean = false, settleMillis: Long = SETTLE_MILLIS) = shot(dark, settleMillis) { AnswerReport(state) }

    private fun show(dark: Boolean = false, reduceMotion: Boolean = false, screen: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MontageTheme(darkTheme = dark, reduceMotion = reduceMotion) { screen() } }
    }

    private fun shot(dark: Boolean, settleMillis: Long = SETTLE_MILLIS, screen: @Composable () -> Unit) {
        show(dark, screen = screen)
        rule.mainClock.advanceTimeBy(settleMillis)
        rule.onRoot().captureRoboImage()
    }

    private fun frame(): Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

    private fun assertMoves(screen: @Composable () -> Unit) {
        show(screen = screen)
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertFalse("움직이지 않는다", first.sameAs(frame()))
    }

    private fun assertStill(screen: @Composable () -> Unit) {
        show(reduceMotion = true, screen = screen)
        val first = frame()
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        assertTrue("동작 줄이기인데 움직인다", first.sameAs(frame()))
    }

    private companion object {
        const val SETTLE_MILLIS = 2_500L
    }
}
