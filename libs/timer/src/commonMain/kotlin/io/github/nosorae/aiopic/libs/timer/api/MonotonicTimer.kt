package io.github.nosorae.aiopic.libs.timer.api

import kotlin.time.Duration

/**
 * 기기가 잠든 동안에도 흐르는 단조 시계. 40분 타이머와 녹음 10초 규칙이 화면을 잠근 채 녹음하는 동안에도 맞아야 한다.
 * 사용자가 시각을 바꿔도 거꾸로 가지 않는다. 테스트에서는 람다로 바꿔 끼운다(`MonotonicTimer { elapsed }`).
 */
public fun interface MonotonicTimer {
    /** 정해진 한 시점(보통 기기 부팅)부터 흐른 시간. 두 값의 차이로만 쓴다. */
    public fun elapsed(): Duration
}
