package io.github.nosorae.aiopic.libs.haptics.api

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/** 화면의 [view] 로 진동을 낸다. Compose 에서는 `LocalView.current` 를 넘긴다. */
public fun androidHaptics(view: View): Haptics = AndroidHaptics(view)

/**
 * 축하를 뺀 효과는 [View.performHapticFeedback] 로 내서 시스템 터치 진동 설정(끄기, 세기)을 그대로 따른다.
 * 축하는 짧은 연속 진동이라 미리 정한 효과가 없어 Composition 으로 만든다. 터치 용도로 내서 같은 설정을 따르게 하려고 API 33 이상에서만 쓰고,
 * 그 아래이거나 기기가 기본 진동 조각을 못 내면 성공 효과로 대신한다.
 */
internal class AndroidHaptics(private val view: View) : Haptics {
    override fun play(effect: HapticEffect) {
        if (effect == HapticEffect.Celebrate && celebrate()) return
        view.performHapticFeedback(feedbackConstant(effect, Build.VERSION.SDK_INT))
    }

    private fun celebrate(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val vibrator = view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return false
        if (!vibrator.areAllPrimitivesSupported(PRIMITIVE_CLICK, PRIMITIVE_TICK)) return false
        // ponytail: 세기와 간격은 손으로 맞춘 값이다. 실기기에서 너무 길거나 약하면 여기만 고친다.
        val effect = VibrationEffect.startComposition()
            .addPrimitive(PRIMITIVE_CLICK, 0.8f)
            .addPrimitive(PRIMITIVE_TICK, 0.6f, 70)
            .addPrimitive(PRIMITIVE_TICK, 0.6f, 70)
            .addPrimitive(PRIMITIVE_CLICK, 1f, 90)
            .compose()
        vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        return true
    }
}

/** 효과마다 시스템 진동 상수. 새 상수가 없는 API 레벨은 가장 비슷한 옛 상수로 대신한다. */
internal fun feedbackConstant(effect: HapticEffect, sdk: Int): Int = when (effect) {
    HapticEffect.Tap -> HapticFeedbackConstants.VIRTUAL_KEY
    HapticEffect.Selection -> if (sdk >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    HapticEffect.RecordStart -> if (sdk >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.LONG_PRESS
    HapticEffect.RecordStop -> if (sdk >= 30) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.CONTEXT_CLICK
    HapticEffect.Success, HapticEffect.Celebrate -> if (sdk >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    HapticEffect.Warning -> HapticFeedbackConstants.LONG_PRESS
    HapticEffect.Error -> if (sdk >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
}
