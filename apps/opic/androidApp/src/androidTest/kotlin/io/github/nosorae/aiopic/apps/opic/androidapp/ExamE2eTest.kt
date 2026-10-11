package io.github.nosorae.aiopic.apps.opic.androidapp

import android.Manifest
import android.os.Process
import android.util.Log
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
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

        click("시작")
        // 서베이(F2.2): 한 화면에 질문 하나, 고르면 바로 다음 화면. 네 묶음에서 묶음 최소를 넘게 합쳐 12개. 난이도 1단계(12문항)
        SURVEY_OPTIONS.forEach(::click)
        SURVEY_GROUPS.forEach { topics ->
            topics.forEach { compose.onNode(hasTestTag("survey-topic-$it")).performScrollTo().performClick() }
            click("다음")
        }
        click("1단계")
        click("다음")

        // 점검과 1번은 내장 문항, 2번은 1번 Play 때 보낸 출제 요청(3초 늦게 답한다)을 1번 답하는 동안 받는다.
        // 세 Play 모두 질문을 만들고 있다는 안내 없이 녹음까지 간다(#150).
        await("장비 점검")
        playWithoutLoading()
        next()
        await("문항 1 / 12")
        playWithoutLoading()
        next()
        await("문항 2 / 12")
        playWithoutLoading()
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
        assertFalse("이어 본 문항에서 다시 듣기가 보인다", exists("다시 듣기"))
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
        // 회차 등급(F5.3)은 마지막 답변 분석 뒤 작업 표가 보낸다. 받으면 기록 화면에 등급 제목과 범위가 보인다
        await("회차별 예상 등급", timeoutMs = ANALYSIS_TIMEOUT)
        await(FAKE_GRADE)

        // 한 문항 연습(F4): 연습 탭의 Play 가 연습 화면을 열고 바로 질문을 읽는다. 답을 내면 그 답변 리포트로 간다
        click("연습")
        answer()
        await("답변 리포트")
        await(FAKE_TRANSCRIPT, timeoutMs = ANALYSIS_TIMEOUT)
    }

    /** 질문을 듣고 녹음이 시작되면 Next 가 켜질 때(10초)까지 기다렸다 낸다. */
    private fun answer() {
        click("Play")
        next()
    }

    /** Play 를 누르고 녹음이 시작될 때까지 질문을 만들고 있다는 안내가 한 번도 안 보여야 한다. 걸린 시간을 로그에 남긴다. */
    private fun playWithoutLoading() {
        val start = System.currentTimeMillis()
        click("Play")
        var sawLoading = false
        compose.waitUntil(QUESTION_TIMEOUT) {
            sawLoading = sawLoading || exists("질문을 만들고 있어요")
            exists("녹음 중")
        }
        Log.i("ExamE2eTest", "FIRST_PLAY Play 에서 녹음 시작까지 ${System.currentTimeMillis() - start}ms")
        assertFalse("Play 가 질문을 불러오느라 기다렸다", sawLoading)
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
        /** Part 1~3 에서 하위 질문이 가장 적은 길. */
        val SURVEY_OPTIONS = listOf("일해 본 적 없어요", "아니요", "수업을 들은 지 5년이 넘었어요", "혼자 집이나 아파트에서")

        /** 여가, 취미, 운동, 휴가 묶음에서 5, 3, 3, 1개(묶음 최소 2, 1, 1, 1 을 채우고 합쳐 12개). 보기 이름은 바뀌어 와서 주제 id 로 찾는다. */
        val SURVEY_GROUPS = listOf(
            listOf("movies", "performances", "concerts", "parks", "camping"),
            listOf("music", "cooking", "drawing"),
            listOf("jogging", "walking", "gym"),
            listOf("overseas-travel"),
        )

        const val SCREEN_TIMEOUT = 15_000L

        /** 기기 음성이 질문을 다 읽고 녹음 10초가 지나기까지. */
        const val QUESTION_TIMEOUT = 60_000L

        /** WorkManager 가 네트워크 조건을 확인하고 대기열을 차례로 비우기까지. */
        const val ANALYSIS_TIMEOUT = 120_000L
    }
}
