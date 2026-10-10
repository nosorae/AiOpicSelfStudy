package io.github.nosorae.aiopic.libs.haptics.api

import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.test.Test

/** 시뮬레이터의 UIKit 진동기로 모든 효과가 예외 없이 불리는지 본다. 축하의 뒤 진동은 메인 큐로 오므로 런루프를 돌린다. */
class IosHapticsTest {
    @Test
    fun everyEffectPlaysWithoutThrowing() {
        val haptics = iosHaptics()
        HapticEffect.entries.forEach(haptics::play)
        NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.5))
    }
}
