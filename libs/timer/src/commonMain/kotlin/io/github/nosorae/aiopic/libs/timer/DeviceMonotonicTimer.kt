package io.github.nosorae.aiopic.libs.timer

import io.github.nosorae.aiopic.libs.timer.api.MonotonicTimer

/** 부팅 뒤 흐른 시간을 재는 기기 시계. 기기가 잠든 동안에도 흐르고, 사용자가 시각을 바꿔도 영향받지 않는다. */
public expect fun deviceMonotonicTimer(): MonotonicTimer
