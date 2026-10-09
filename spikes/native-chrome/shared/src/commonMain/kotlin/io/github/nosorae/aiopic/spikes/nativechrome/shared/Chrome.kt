package io.github.nosorae.aiopic.spikes.nativechrome.shared

/** 화면이 앱 틀에서 어떤 대우를 받는지. 화면 모듈은 이 값을 모른다. */
enum class Chrome { TopLevel, Sub, FullScreen }

enum class Platform { Android, Ios }

/** 앱 틀의 화면 경로 표. 하위 화면 탭 표시 차이는 이 표와 [showsTabBar] 한 곳에서만 갈린다. */
enum class Screen(
    val title: String,
    val chrome: Chrome,
) {
    Home("홈", Chrome.TopLevel),
    History("기록", Chrome.TopLevel),
    Settings("설정", Chrome.TopLevel),
    ReportDetail("리포트 상세", Chrome.Sub),
    Exam("시험 진행", Chrome.FullScreen),
}

fun Screen.showsTabBar(platform: Platform): Boolean = when (chrome) {
    Chrome.TopLevel -> true
    Chrome.Sub -> platform == Platform.Ios
    Chrome.FullScreen -> false
}

val topLevelScreens: List<Screen> = Screen.entries.filter { it.chrome == Chrome.TopLevel }
