package io.github.nosorae.aiopic.ui.featuresettings.practice

import dev.zacsweers.metro.Inject

@Inject
class SetAnswerTimerUseCase(private val answerTimerStore: AnswerTimerStore) {
    suspend operator fun invoke(on: Boolean) = answerTimerStore.setAnswerTimerOn(on)
}
