package io.github.nosorae.aiopic.apps.opic.shared

import io.github.nosorae.aiopic.ui.featureonboarding.start.StartDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class AppRouteTest {
    private val subScreens = listOf(AppRoute.ReportDetail("s"), AppRoute.AnswerReport("a"), AppRoute.KeyEntry("gemini"), AppRoute.FreeTierNotice, AppRoute.FlagExport)
    private val everyRoute = AppRoute.tabs + subScreens + listOf(AppRoute.Exam("s"), AppRoute.Practice("a"), AppRoute.StartKeyEntry, AppRoute.StartConsent)

    private fun tabBar(platform: Platform) = everyRoute.filter { it.showsTabBar(platform) }

    @Test
    fun `하단 탭은 홈 기록 설정 셋이다`() {
        assertEquals(listOf(AppRoute.Home, AppRoute.History, AppRoute.Settings), AppRoute.tabs)
    }

    @Test
    fun `Android 는 최상위 탭 화면에서만 탭을 보인다`() {
        assertEquals(AppRoute.tabs, tabBar(Platform.Android))
    }

    @Test
    fun `iOS 는 리포트 상세 같은 하위 화면에도 탭을 남긴다`() {
        assertEquals(AppRoute.tabs + subScreens, tabBar(Platform.Ios))
    }

    @Test
    fun `시험 진행 녹음 첫 실행 화면은 두 플랫폼 모두 탭이 없다`() {
        val fullScreens = listOf(AppRoute.Exam("s"), AppRoute.Practice("a"), AppRoute.StartKeyEntry, AppRoute.StartConsent)

        assertEquals(emptyList(), fullScreens.filter { it.showsTabBar(Platform.Android) || it.showsTabBar(Platform.Ios) })
    }

    @Test
    fun `시스템 뒤로가기를 막는 화면은 시험 하나다`() {
        assertEquals(listOf<AppRoute>(AppRoute.Exam("s")), everyRoute.filter { it.blocksBack })
    }

    @Test
    fun `iOS 뒤로 버튼은 하위 화면과 한 문항 연습에만 있고 시험과 첫 실행 화면에는 없다`() {
        assertEquals(subScreens + AppRoute.Practice("a"), everyRoute.filter { it.showsBackButtonOnIos })
    }

    @Test
    fun `F1_0 시작 분기 결과마다 첫 화면이 정해진다`() {
        assertEquals(
            listOf(AppRoute.StartKeyEntry, AppRoute.StartConsent, AppRoute.Home),
            listOf(StartDestination.KeyEntry, StartDestination.Consent, StartDestination.Home).map(AppRoute::start),
        )
    }
}
