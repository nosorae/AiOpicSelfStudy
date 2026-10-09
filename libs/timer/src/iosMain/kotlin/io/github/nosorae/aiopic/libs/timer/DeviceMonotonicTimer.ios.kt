package io.github.nosorae.aiopic.libs.timer

import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime_nsec_np
import kotlin.time.Duration.Companion.nanoseconds

// Darwin 의 CLOCK_MONOTONIC 은 잠든 동안에도 흐른다. CLOCK_UPTIME_RAW 는 멈추므로 쓰지 않는다.
public actual fun deviceMonotonicTimer(): MonotonicTimer = MonotonicTimer { clock_gettime_nsec_np(CLOCK_MONOTONIC.toUInt()).toLong().nanoseconds }
