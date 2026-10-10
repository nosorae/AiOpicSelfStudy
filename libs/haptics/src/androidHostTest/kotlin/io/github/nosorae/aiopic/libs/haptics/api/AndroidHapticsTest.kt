package io.github.nosorae.aiopic.libs.haptics.api

import android.content.Context
import android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** 효과마다 시스템 터치 진동 상수를 쓰는지, 축하가 지원 기기에서만 연속 진동을 내는지 본다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidHapticsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val view = View(context)
    private val vibrator = shadowOf(context.getSystemService(VibratorManager::class.java).defaultVibrator)

    @Test
    fun successUsesSystemConfirmFeedback() {
        androidHaptics(view).play(HapticEffect.Success)

        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(view).lastHapticFeedbackPerformed())
    }

    @Test
    fun celebrateComposesPrimitivesWhenDeviceSupportsThem() {
        vibrator.setSupportedPrimitives(listOf(PRIMITIVE_CLICK, PRIMITIVE_TICK))

        androidHaptics(view).play(HapticEffect.Celebrate)

        assertTrue(vibrator.isVibrating)
        assertNotEquals(HapticFeedbackConstants.CONFIRM, shadowOf(view).lastHapticFeedbackPerformed())
    }

    @Test
    fun celebrateFallsBackToConfirmWithoutPrimitives() {
        androidHaptics(view).play(HapticEffect.Celebrate)

        assertFalse(vibrator.isVibrating)
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(view).lastHapticFeedbackPerformed())
    }

    @Test
    fun olderApiLevelsUseClosestOldConstant() {
        assertEquals(HapticFeedbackConstants.SEGMENT_TICK, feedbackConstant(HapticEffect.Selection, sdk = 34))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, feedbackConstant(HapticEffect.Selection, sdk = 33))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, feedbackConstant(HapticEffect.Error, sdk = 29))
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, feedbackConstant(HapticEffect.Success, sdk = 28))
    }
}
