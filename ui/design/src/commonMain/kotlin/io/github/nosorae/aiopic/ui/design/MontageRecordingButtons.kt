package io.github.nosorae.aiopic.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import kotlin.time.Duration

/**
 * 문항 녹음 중 아래 버튼 둘(연습과 시험이 같이 쓴다). 다시 듣기는 쓸 수 있는 [replayWindow] 동안 채움이 줄어들고,
 * Next 는 켜지기까지 [nextDelay] 동안 채움이 차오른다. 다 차서 Next 가 켜지면 누름 진동을 한 번 낸다.
 * 동작 줄이기면 Next 는 켜지기까지 남은 초를 글자로 보인다. [tagPrefix] 를 주면 `<prefix>-replay`, `<prefix>-next` 테스트 태그를 단다.
 */
// ponytail: 앱이 한국어 하나라 버튼 이름을 여기 둔다. 언어를 더하면 리소스로 옮긴다.
@Composable
fun MontageRecordingButtons(
    recorded: Duration,
    replayWindow: Duration,
    nextDelay: Duration,
    canReplay: Boolean,
    canNext: Boolean,
    saving: Boolean,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String? = null,
) {
    val replayLeft = rememberElapsedFraction(recorded, replayWindow, running = canReplay)
    val nextFill = rememberElapsedFraction(recorded, nextDelay, running = !canNext)
    val haptics = LocalHaptics.current
    val wasNext = remember { booleanArrayOf(canNext) }
    LaunchedEffect(canNext) {
        if (canNext && !wasNext[0]) haptics.play(HapticEffect.Tap)
        wasNext[0] = canNext
    }
    fun Modifier.tag(name: String) = if (tagPrefix != null) testTag("$tagPrefix-$name") else this
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        MontageButton(
            text = "다시 듣기",
            onClick = onReplay,
            enabled = canReplay,
            type = MontageButtonType.Assistive,
            fill = if (canReplay) ({ 1f - replayLeft.value }) else null,
            modifier = Modifier.weight(1f).tag("replay"),
        )
        MontageButton(
            text = if (MontageMotion.reduceMotion && !canNext) "Next (${(nextDelay - recorded).inWholeSeconds}초)" else "Next",
            onClick = onNext,
            enabled = canNext,
            loading = saving,
            fill = if (canNext) null else ({ nextFill.value }),
            modifier = Modifier.weight(1f).tag("next"),
        )
    }
}
