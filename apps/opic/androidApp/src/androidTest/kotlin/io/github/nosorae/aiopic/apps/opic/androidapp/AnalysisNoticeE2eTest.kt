package io.github.nosorae.aiopic.apps.opic.androidapp

import android.Manifest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.github.nosorae.aiopic.apps.opic.shared.AnalysisOutcome
import io.github.nosorae.aiopic.apps.opic.shared.AppRoute
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 분석 중에 앱을 내리면 알림창에 진행이 보이고, 끝나면 끝 알림이 뜨고, 누르면 그 리포트가 열리는지 실제 앱으로 본다(#229).
 * 계측 인자 `answerDelay` 로 답변 분석 응답을 늦춘다. 그 사이 홈으로 나가 알림창을 연다.
 * 알림창은 앱 밖이라 UI Automator 로 열고 누른다. 화면 사진은 기기의 `/data/local/tmp/notice-*.png` 에 남긴다.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisNoticeE2eTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun progressThenDoneNoticeOpensReport() {
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.RECORD_AUDIO)
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        ActivityScenario.launch(MainActivity::class.java)

        click("Gemini")
        compose.onNode(hasSetTextAction()).performTextInput("test-key")
        click("키 확인하고 저장")
        click("동의하고 계속하기")

        // 연습 답변 하나를 낸다. 분석 응답은 answerDelay 만큼 늦게 온다.
        click("연습")
        click("Play")
        compose.waitUntil(QUESTION_TIMEOUT) { compose.onAllNodes(hasText("Next") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Next") and isEnabled()).performClick()

        device.pressHome()
        device.openNotification()
        assertNotNull("앱을 내린 뒤 알림창에 분석 진행이 보이지 않는다", device.wait(Until.findObject(By.textContains("답변을 분석하고 있어요")), SCREEN_TIMEOUT))
        // 소리 없는 진행 알림은 알림창에서 한 줄로 접혀 몇 개 끝났는지는 펼쳐야 보인다. 글은 AnalysisNoticeTest 가 본다.
        shot("progress")

        val done = device.wait(Until.findObject(By.text("리포트가 준비됐어요")), ANALYSIS_TIMEOUT)
        assertNotNull("분석이 끝나도 끝 알림이 뜨지 않는다", done)
        assertTrue("끝났는데 진행 알림이 남아 있다", device.wait(Until.gone(By.textContains("답변을 분석하고 있어요")), SCREEN_TIMEOUT))
        shot("done")
        done.click()

        // 알림이 연 답변 리포트에 분석 결과가 보이고, 뒤로 가면 모의고사 탭이다.
        // 앱 화면은 Compose 규칙으로 기다린다. 이 규칙이 화면 그리기를 맡아서 기다리는 동안에만 새 화면이 그려진다.
        await("답변 리포트")
        await(FAKE_TRANSCRIPT)
        shot("report")
        device.pressBack()
        await("시작")

        // 모의고사 답변의 끝 알림은 회차 리포트를 연다. 같은 알림 경로에 회차 경로를 실어 띄우고 알림창에서 누른다.
        device.pressHome()
        AnalysisNotifications.notify(app, AnalysisOutcome(AppRoute.ReportDetail("e2e-session"), failed = false))
        device.openNotification()
        device.wait(Until.findObject(By.text("리포트가 준비됐어요")), SCREEN_TIMEOUT).click()
        await("회차 리포트")
        await("이 회차를 찾을 수 없습니다.")
        shot("session-report")
    }

    private fun click(text: String) {
        compose.waitUntil(SCREEN_TIMEOUT) { compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(text) and isEnabled()).onFirst().performClick()
    }

    private fun await(text: String) = compose.waitUntil(SCREEN_TIMEOUT) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun shot(name: String) {
        device.executeShellCommand("screencap -p /data/local/tmp/notice-$name.png")
    }

    private companion object {
        const val SCREEN_TIMEOUT = 15_000L
        const val QUESTION_TIMEOUT = 60_000L
        const val ANALYSIS_TIMEOUT = 120_000L
    }
}
