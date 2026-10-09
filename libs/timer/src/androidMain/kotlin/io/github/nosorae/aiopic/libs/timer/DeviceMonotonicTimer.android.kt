package io.github.nosorae.aiopic.libs.timer

import android.os.SystemClock
import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import kotlin.time.Duration.Companion.nanoseconds

// uptimeMillis 는 잠든 동안 멈추므로 화면을 잠근 채 녹음할 때 40분 타이머가 늦어진다. 잠든 시간까지 세는 쪽을 쓴다.
public actual fun deviceMonotonicTimer(): MonotonicTimer = MonotonicTimer { SystemClock.elapsedRealtimeNanos().nanoseconds }
