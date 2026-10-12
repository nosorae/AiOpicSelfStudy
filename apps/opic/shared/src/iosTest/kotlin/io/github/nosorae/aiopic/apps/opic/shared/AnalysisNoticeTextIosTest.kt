package io.github.nosorae.aiopic.apps.opic.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 알림 문구는 Compose 리소스에서 읽는다. 호스트 JVM 테스트는 Android 리소스가 없어 못 읽으므로 iOS 에서 본다. */
class AnalysisNoticeTextIosTest {
    @Test
    fun `229 진행 알림 문구는 숫자에 단위를 붙여 몇 개 중 몇 개가 끝났는지 쓴다`() = runTest {
        assertEquals(
            ProgressNotice(3, 15, "답변을 분석하고 있어요", "15개 중 3개 끝. 문법 교정과 한 단계 위 표현이 곧 나와요"),
            AnalysisProgress(done = 3, total = 15).notice(),
        )
    }

    @Test
    fun `229 끝 알림 문구는 성공과 실패를 나눠 쓴다`() = runTest {
        val ready = AnalysisOutcome(AppRoute.ReportDetail("s1"), failed = false)
        val failed = ready.copy(failed = true)

        assertEquals("리포트가 준비됐어요", ready.title())
        assertEquals("분석하지 못했어요", failed.title())
        assertEquals("리포트에서 이유를 보고 다시 분석할 수 있어요", failed.text())
    }
}
