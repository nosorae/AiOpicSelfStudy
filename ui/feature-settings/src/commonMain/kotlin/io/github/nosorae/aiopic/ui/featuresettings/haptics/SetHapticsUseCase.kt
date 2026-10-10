package io.github.nosorae.aiopic.ui.featuresettings.haptics

import dev.zacsweers.metro.Inject

@Inject
class SetHapticsUseCase(private val hapticsStore: HapticsStore) {
    suspend operator fun invoke(on: Boolean) = hapticsStore.setHapticsOn(on)
}
