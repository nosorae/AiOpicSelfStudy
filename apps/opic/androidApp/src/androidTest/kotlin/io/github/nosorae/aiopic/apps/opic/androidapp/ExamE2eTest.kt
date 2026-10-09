package io.github.nosorae.aiopic.apps.opic.androidapp

import android.Manifest
import android.os.Process
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * 모의시험 한 회를 실제 앱으로 끝까지 돈다(완료 기준). Gemini 만 저장한 응답이고 녹음기, 기기 음성, Room, WorkManager 는 기기의 실제 것이다.
 *
 * 두 테스트는 순서대로 이어진다. Test Orchestrator 가 테스트마다 계측을 새로 띄우고, 계측이 끝나면 시스템이 앱 프로세스를 강제 종료한다.
 * 그래서 첫 테스트는 2번 문항을 녹음하는 도중에 끝나고, 둘째 테스트는 새 프로세스에서 앱을 다시 켜 이어 본다(F3).
 * 앱 데이터는 설치할 때 비고 테스트 사이에는 지우지 않는다.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExamE2eTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext

    /** 첫 테스트의 프로세스 id. 둘째 테스트가 프로세스가 바뀌었는지 본다. */
    private val pidFile = File(app.filesDir, "e2e-pid")
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launch() {
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.RECORD_AUDIO)
        // 앞 테스트의 프로세스가 끝날 때 화면이 그대로 남도록 닫지 않는다. 계측이 끝나면 시스템이 프로세스째 지운다.
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun step1_firstRunThenKilledWhileRecordingQuestion2() {
        // 첫 실행(F1.0): 키 넣기, 동의
        click("Gemini")
        compose.onNode(hasSetTextAction()).performTextInput("test-key")
        click("키 확인하고 저장")
        click("동의하고 계속하기")

        click("모의시험 시작")
        // 서베이 주제 12개 이상(F2.2), 난이도 1단계(12문항)
        SURVEY_TOPICS.forEach { compose.onNode(hasText(it)).performScrollTo().performClick() }
        click("다음")
        click("1단계")
        click("다음")

        await("장비 점검")
        answer()
        await("문항 1 / 12")
        answer()
        await("문항 2 / 12")
        click("Play")
        await("녹음 중")
        // 녹음 조각은 약 5초마다 끊긴다. 하나는 온전히 남게 기다린 뒤 녹음 중에 끝낸다.
        Thread.sleep(8_000)
        pidFile.writeText(Process.myPid().toString())
    }

    @Test
    fun step2_resumeAfterKillThenFinishReportHistoryPractice() {
        assertNotEquals("앞 테스트와 같은 프로세스다. 강제 종료가 일어나지 않았다", pidFile.readText(), Process.myPid().toString())

        // 이어 보기(F3): 같은 회차의 2번 문항에서 다시 시작하고, 이어 녹음은 다시 듣기를 쓴 것으로 센다(F3.3)
        click("이어 보기")
        await("문항 2 / 12")
        click("Play")
        await("녹음 중")
        compose.onNode(hasText("다시 듣기") and isNotEnabled()).assertExists()
        next()

        repeat(10) {
            await("문항 ${it + 3} / 12", "난이도 재조정")
            if (exists("난이도 재조정")) {
                click("비슷한 질문")
                click("다음")
            }
            await("문항 ${it + 3} / 12")
            answer()
        }

        // 리포트(F7.2): 답변 12개가 저장한 Gemini 응답으로 분석된다
        await("답변 12개")
        await("${FAKE_SCORE}점", timeoutMs = ANALYSIS_TIMEOUT)
        click("1번 자기소개")
        await(FAKE_TRANSCRIPT)
        back()

        back()
        click("기록")
        await("회차별 예상 등급")

        // 한 문항 연습(F4): 답을 내면 그 답변 리포트로 간다
        click("홈")
        click("한 문항 풀기")
        answer()
        await("답변 리포트")
        await(FAKE_TRANSCRIPT, timeoutMs = ANALYSIS_TIMEOUT)
    }

    /** 질문을 듣고 녹음이 시작되면 Next 가 켜질 때(10초)까지 기다렸다 낸다. */
    private fun answer() {
        click("Play")
        next()
    }

    private fun next() {
        waitFor(hasText("Next") and isEnabled(), QUESTION_TIMEOUT)
        compose.onNode(hasText("Next") and isEnabled()).performClick()
    }

    private fun click(text: String) {
        waitFor(hasText(text) and isEnabled(), SCREEN_TIMEOUT)
        compose.onAllNodes(hasText(text) and isEnabled()).onFirst().performClick()
    }

    private fun back() = scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

    private fun exists(text: String) = compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    /** [texts] 가운데 하나가 보일 때까지 기다린다. */
    private fun await(vararg texts: String, timeoutMs: Long = SCREEN_TIMEOUT) = compose.waitUntil(timeoutMs) { texts.any(::exists) }

    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long) = compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private companion object {
        /** 시험 팩의 서베이 주제 14개 가운데 앞 12개. */
        val SURVEY_TOPICS = listOf("movies", "music", "parks", "cafes", "shopping", "overseas-travel", "home", "jogging", "tv", "walking", "cooking", "domestic-travel")

        const val SCREEN_TIMEOUT = 15_000L

        /** 기기 음성이 질문을 다 읽고 녹음 10초가 지나기까지. */
        const val QUESTION_TIMEOUT = 60_000L

        /** WorkManager 가 네트워크 조건을 확인하고 대기열을 차례로 비우기까지. */
        const val ANALYSIS_TIMEOUT = 120_000L
    }
}
