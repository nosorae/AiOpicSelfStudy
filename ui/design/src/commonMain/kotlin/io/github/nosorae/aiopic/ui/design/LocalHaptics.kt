package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nosorae.aiopic.libs.haptics.api.Haptics

/**
 * 화면이 진동을 낼 때 꺼내 쓴다. 앱 틀이 플랫폼 진동에 설정의 켜기/끄기를 씌워 넣는다.
 * 기본값은 아무것도 안 해서 미리보기와 스크린샷 테스트는 따로 넣지 않아도 된다.
 * 진동은 거의 모든 화면이 쓰는데 화면마다 인자로 내려보내면 화면 함수 서명이 모두 바뀌어서 이것 하나만 CompositionLocal 로 둔다.
 */
val LocalHaptics = staticCompositionLocalOf { Haptics.None }
