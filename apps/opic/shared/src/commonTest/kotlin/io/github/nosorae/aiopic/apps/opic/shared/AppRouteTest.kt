package io.github.nosorae.aiopic.apps.opic.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class AppRouteTest {
    private val everyRoute = listOf(AppRoute.Home, AppRoute.History, AppRoute.Settings, AppRoute.ReportDetail("s"), AppRoute.Exam, AppRoute.Practice, AppRoute.KeyEntry("gemini"), AppRoute.FreeTierNotice)

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
        assertEquals(AppRoute.tabs + listOf(AppRoute.ReportDetail("s"), AppRoute.KeyEntry("gemini"), AppRoute.FreeTierNotice), tabBar(Platform.Ios))
    }

    @Test
    fun `시험 진행과 녹음 화면은 두 플랫폼 모두 탭이 없다`() {
        val fullScreens = listOf(AppRoute.Exam, AppRoute.Practice)

        assertEquals(emptyList(), fullScreens.filter { it.showsTabBar(Platform.Android) || it.showsTabBar(Platform.Ios) })
    }

    @Test
    fun `시스템 뒤로가기를 막는 화면은 시험 하나다`() {
        assertEquals(listOf<AppRoute>(AppRoute.Exam), everyRoute.filter { it.blocksBack })
    }

    @Test
    fun `iOS 뒤로 버튼은 하위 화면과 한 문항 연습에만 있고 시험에는 없다`() {
        assertEquals(
            listOf(AppRoute.ReportDetail("s"), AppRoute.Practice, AppRoute.KeyEntry("gemini"), AppRoute.FreeTierNotice),
            everyRoute.filter { it.showsBackButtonOnIos },
        )
    }
}
