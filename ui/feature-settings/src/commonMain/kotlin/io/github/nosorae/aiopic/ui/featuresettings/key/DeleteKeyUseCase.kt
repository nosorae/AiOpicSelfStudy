package io.github.nosorae.aiopic.ui.featuresettings.key

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.key.KeyProvider

/** 고른 제공사 키 하나만 지운다(F1.2). 다른 제공사 키는 남는다. */
@Inject
class DeleteKeyUseCase(private val keyVault: KeyVault) {
    suspend operator fun invoke(provider: KeyProvider) = keyVault.delete(provider.id)
}
