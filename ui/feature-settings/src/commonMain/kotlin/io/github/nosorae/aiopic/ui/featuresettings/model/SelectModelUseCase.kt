package io.github.nosorae.aiopic.ui.featuresettings.model

import dev.zacsweers.metro.Inject

@Inject
class SelectModelUseCase(private val modelSelectionStore: ModelSelectionStore) {
    suspend operator fun invoke(id: String) = modelSelectionStore.selectModel(id)
}
