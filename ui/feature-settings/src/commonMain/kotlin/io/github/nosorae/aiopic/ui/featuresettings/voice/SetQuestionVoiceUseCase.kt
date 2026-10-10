package io.github.nosorae.aiopic.ui.featuresettings.voice

import dev.zacsweers.metro.Inject

@Inject
class SetQuestionVoiceUseCase(private val questionVoiceStore: QuestionVoiceStore) {
    suspend operator fun invoke(naturalVoiceOn: Boolean) = questionVoiceStore.setNaturalVoiceOn(naturalVoiceOn)
}
