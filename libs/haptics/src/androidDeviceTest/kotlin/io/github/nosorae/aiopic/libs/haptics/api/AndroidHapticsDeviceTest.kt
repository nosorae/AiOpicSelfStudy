package io.github.nosorae.aiopic.libs.haptics.api

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 기기의 View 와 Vibrator 로 모든 효과가 예외 없이 불리는지 본다. 손 느낌은 사람이 실기기에서 본다. */
@RunWith(AndroidJUnit4::class)
class AndroidHapticsDeviceTest {
    @Test
    fun everyEffectPlaysWithoutThrowing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val haptics = androidHaptics(View(instrumentation.targetContext))
            HapticEffect.entries.forEach(haptics::play)
        }
    }
}
