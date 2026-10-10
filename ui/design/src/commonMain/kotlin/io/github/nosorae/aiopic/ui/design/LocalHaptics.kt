package io.github.nosorae.aiopic.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.libs.haptics.api.Haptics

/**
 * 화면이 진동을 낼 때 꺼내 쓴다. 앱 틀이 플랫폼 진동에 설정의 켜기/끄기를 씌워 넣는다.
 * 기본값은 아무것도 안 해서 미리보기와 스크린샷 테스트는 따로 넣지 않아도 된다.
 * 진동은 거의 모든 화면이 쓰는데 화면마다 인자로 내려보내면 화면 함수 서명이 모두 바뀌어서 이것 하나만 CompositionLocal 로 둔다.
 */
val LocalHaptics = staticCompositionLocalOf { Haptics.None }

/**
 * [recording] 이 켜지면 녹음 시작, 꺼지면 녹음 끝 진동을 낸다. 화면을 보지 않고 말하는 중에도 녹음이 켜지고 꺼진 것을 손으로 안다.
 * 처음 그릴 때와 화면을 돌려 다시 그릴 때는 바뀐 것이 없어 내지 않는다.
 */
@Composable
fun RecordHaptics(recording: Boolean) {
    val haptics = LocalHaptics.current
    val last = remember { booleanArrayOf(recording) }
    LaunchedEffect(recording) {
        if (last[0] == recording) return@LaunchedEffect
        last[0] = recording
        haptics.play(if (recording) HapticEffect.RecordStart else HapticEffect.RecordStop)
    }
}
