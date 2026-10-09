package io.github.nosorae.aiopic.apps.opic.shared

import kotlinx.serialization.Serializable

enum class Platform { Android, Ios }

/** 앱 틀이 화면을 어떻게 대우하는지. 화면 모듈은 이 값을 모른다. */
private enum class Chrome { TopLevel, Sub, FullScreen }

/**
 * 앱 틀의 화면 경로 표. 하단 탭 표시, 뒤로가기 가로채기, iOS 뒤로 버튼이 이 표 한 곳에서만 갈린다(ADR 0018).
 * 화면 모듈은 플랫폼도 탭도 모르고, 두 플랫폼 앱 틀(Android `AppShell`, iOS `ContentView.swift`)이 이 표를 읽는다.
 *
 * Swift 의 NavigationStack 경로로도 쓰므로 interface 가 아니라 class 다. Kotlin 클래스는 Swift 에서 Hashable 이다.
 *
 * architecture 5절(키는 ui/navigation 의 NavKey, 등록은 화면 모듈 함수)과 다르다. 탭과 뒤로가기 표가 키에 붙어 있어 앱 틀에 두고,
 * iOS 는 Navigation 3 가 아니라 SwiftUI NavigationStack 이라 화면 모듈의 Navigation 3 등록 함수를 함께 쓸 수 없다. 맞출지는 후속 이슈에서 정한다.
 */
@Serializable
sealed class AppRoute {
    @Serializable data object Home : AppRoute()

    @Serializable data object History : AppRoute()

    @Serializable data object Settings : AppRoute()

    @Serializable data class ReportDetail(val sessionId: String) : AppRoute()

    @Serializable data object Exam : AppRoute()

    /** 한 문항 연습. 녹음하는 화면이라 탭이 없지만 시험이 아니라서 뒤로가기는 막지 않는다. */
    @Serializable data object Practice : AppRoute()

    /** 설정의 제공사별 키 관리에서 여는 키 입력. [provider] 는 제공사 이름(`gemini`)이다. */
    @Serializable data class KeyEntry(val provider: String) : AppRoute()

    /** 설정의 무료 등급 안내. 음성 전송 동의 화면을 그대로 연다(F1.3). */
    @Serializable data object FreeTierNotice : AppRoute()

    private val chrome: Chrome
        get() = when (this) {
            Home, History, Settings -> Chrome.TopLevel
            is ReportDetail, is KeyEntry, FreeTierNotice -> Chrome.Sub
            Exam, Practice -> Chrome.FullScreen
        }

    val title: String
        get() = when (this) {
            Home -> "홈"
            History -> "기록"
            Settings -> "설정"
            is ReportDetail -> "회차 리포트"
            Exam -> "모의시험"
            Practice -> "한 문항 연습"
            is KeyEntry -> "API 키 넣기"
            FreeTierNotice -> "음성 전송 동의"
        }

    /** 시스템 뒤로가기로 나가면 안 되는 화면. 시험 중에는 저장하고 나가는 화면 안 버튼으로만 나간다. */
    val blocksBack: Boolean get() = this == Exam

    /** Android 는 탭 화면에서만, iOS 는 하위 화면에도 탭을 보인다. 시험과 녹음 화면은 두 플랫폼 모두 탭이 없다. */
    fun showsTabBar(platform: Platform): Boolean = when (chrome) {
        Chrome.TopLevel -> true
        Chrome.Sub -> platform == Platform.Ios
        Chrome.FullScreen -> false
    }

    /** iOS 내비게이션 바(뒤로 버튼과 가장자리 스와이프)를 보일지. Android 는 시스템 뒤로가기라 그리는 버튼이 없다. */
    val showsBackButtonOnIos: Boolean get() = chrome != Chrome.TopLevel && !blocksBack

    companion object {
        /** 하단 탭 순서. 값으로 두면 하위 object 초기화와 순환해 null 이 들어가므로 읽을 때 만든다. */
        val tabs: List<AppRoute> get() = listOf(Home, History, Settings)
    }
}
