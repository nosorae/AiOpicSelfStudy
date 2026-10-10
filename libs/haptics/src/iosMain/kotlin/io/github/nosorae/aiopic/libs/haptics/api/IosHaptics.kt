package io.github.nosorae.aiopic.libs.haptics.api

import platform.Foundation.NSThread
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/** iOS 진동. 시스템 설정의 "시스템 햅틱"을 끄면 느껴지지 않는다. */
public fun iosHaptics(): Haptics = IosHaptics()

/** UIKit 진동은 메인 스레드에서만 내므로 다른 스레드에서 부르면 메인으로 넘긴다. */
// ponytail: 부를 때마다 진동기를 새로 만들고 prepare 하지 않는다. 첫 진동이 늦게 느껴지면 진동기를 들고 있다가 prepare 한다.
internal class IosHaptics : Haptics {
    override fun play(effect: HapticEffect) {
        if (NSThread.isMainThread) perform(effect) else dispatch_async(dispatch_get_main_queue()) { perform(effect) }
    }

    private fun perform(effect: HapticEffect) {
        when (effect) {
            HapticEffect.Tap -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

            HapticEffect.Selection -> UISelectionFeedbackGenerator().selectionChanged()

            HapticEffect.RecordStart, HapticEffect.RecordStop -> impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

            HapticEffect.Success -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

            HapticEffect.Warning -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)

            HapticEffect.Error -> notify(UINotificationFeedbackType.UINotificationFeedbackTypeError)

            HapticEffect.Celebrate -> {
                notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
                // ponytail: 간격은 손으로 맞춘 값이다. 실기기에서 어색하면 여기만 고친다.
                for (step in 1..2) {
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, step * CELEBRATE_GAP_NS), dispatch_get_main_queue()) {
                        impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                    }
                }
            }
        }
    }

    private fun impact(style: UIImpactFeedbackStyle) = UIImpactFeedbackGenerator(style).impactOccurred()

    private fun notify(type: UINotificationFeedbackType) = UINotificationFeedbackGenerator().notificationOccurred(type)

    private companion object {
        const val CELEBRATE_GAP_NS = 120_000_000L
    }
}
