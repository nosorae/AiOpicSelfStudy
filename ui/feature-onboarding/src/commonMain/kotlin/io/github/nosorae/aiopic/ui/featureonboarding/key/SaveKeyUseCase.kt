package io.github.nosorae.aiopic.ui.featureonboarding.key

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult

/** 키를 확인하고 맞을 때만 제공사 이름으로 저장한다(F1.1, F1.2). 틀린 키는 남기지 않는다. */
@Inject
class SaveKeyUseCase(
    private val keyVault: KeyVault,
    private val keyChecker: KeyChecker,
) {
    suspend operator fun invoke(provider: KeyProvider, key: String): DomainResult<Unit> {
        val trimmed = key.trim()
        val result = keyChecker.check(provider, trimmed)
        if (result is DomainResult.Success) keyVault.put(provider.id, trimmed)
        return result
    }
}
