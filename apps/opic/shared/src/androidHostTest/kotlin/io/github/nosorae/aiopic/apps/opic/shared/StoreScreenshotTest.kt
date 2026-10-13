package io.github.nosorae.aiopic.apps.opic.shared

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.question.pick.parseQuestions
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.packs.opicen.OpicEn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Play 스토어 폰 스크린샷(#236). 예시 기록([seedStoreDemo])을 넣은 앱 틀을 1080x2160 으로 찍어 fastlane 이 올리는 폴더에 둔다.
 * `check` 가 다른 기준 이미지처럼 비교하고, 다시 찍을 때는 `./gradlew recordRoborazziAndroidHostTest` 다.
 * 테스트 그래프라 디버깅 도구가 설치되지 않아 설정의 개발자 도구 줄도 없다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val graph = testAppGraph()

    @Before
    fun seed() = runBlocking { graph.opicStore.seedStoreDemo(parseQuestions(File(POOL).readText())) }

    @Test
    fun `예시 기록은 등급이 오르는 끝난 회차 셋과 연습 둘이고 답변 리포트용 답에는 교정과 최종 스크립트가 있다`() = runBlocking {
        val sessions = graph.opicStore.sessions().first()
        assertEquals(listOf("IM1", "IM2", "IM3"), sessions.map { it.grade?.low })
        assertTrue(sessions.all { it.status == ExamStatus.Finished && it.answerIds.size == 15 })
        val answers = graph.opicStore.answers().first()
        assertEquals(2, answers.count { it.examSessionId == null })
        val showcase = (answers.single { it.id == SHOWCASE }.analysis as AnswerAnalysis.Done).feedback
        assertEquals("parks-description-1", answers.single { it.id == SHOWCASE }.question.id)
        assertTrue(showcase.corrections.all { it.original in showcase.transcript })
        assertTrue(showcase.contentFixes.all { it.original in showcase.transcript } && showcase.finalScript != null)
    }

    @Test fun shot1_examTab() = shot("1_exam_tab", "모의고사 3회차", AppRoute.ExamTab)

    @Test
    fun shot2_question() {
        // 진행 중인 회차의 3번 문항. 녹음 전이라 Play 를 기다린다.
        val third = OpicEn.examSpec.steps.withIndex().filter { it.value is ExamStep.Ask }[2].index
        runBlocking { graph.opicStore.save(ExamSession(id = "now", stepIndex = third, surveyTopics = listOf("parks"), level = 4, remaining = 34.minutes + 12.seconds)) }
        shot("2_question", "문항 3 / 15", AppRoute.ExamTab, AppRoute.Exam("now"))
    }

    @Test fun shot3_sessionReport() = shot("3_session_report", "IM3 ~ IH", AppRoute.ExamTab, AppRoute.ReportDetail("demo-3"))

    @Test fun shot4_answerReport() = shot("4_answer_report", "It has a lake", AppRoute.ExamTab, AppRoute.ReportDetail("demo-3"), AppRoute.AnswerReport(SHOWCASE))

    @Test fun shot5_history() = shot("5_history", "IM1 ~ IM2", AppRoute.ExamTab, AppRoute.History)

    /** 데이터가 그려질 때까지 기다리고, 움직임(등급 구슬, 점수 숫자)이 멈춘 끝 장면을 찍는다. */
    private fun shot(name: String, waitFor: String, vararg stack: AppRoute) {
        rule.setContent { AppShell(graph, startStack = stack.toList()) }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText(waitFor, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(3_000)
        rule.onRoot().captureRoboImage("$PHONE_SCREENSHOTS/$name.png")
    }

    private companion object {
        // 테스트는 모듈 폴더(apps/opic/shared)에서 돈다.
        const val POOL = "../../../packs/opic-en/src/commonMain/composeResources/files/questions/pool.json"
        const val PHONE_SCREENSHOTS = "../../../fastlane/metadata/android/ko-KR/images/phoneScreenshots"
        const val SHOWCASE = "demo-3-2"
    }
}
