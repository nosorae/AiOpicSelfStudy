package io.github.nosorae.aiopic.ui.featureonboarding.key

import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.DomainError
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult

// ponytail: libs/keyvault 의 commonTest 에도 같은 Fake 가 있다. 세 번째 모듈이 쓰게 되면 공용 테스트 모듈로 옮긴다.
class FakeKeyVault : KeyVault {
    private val keys = mutableMapOf<String, String>()

    override suspend fun get(provider: String): String? = keys[provider]

    override suspend fun put(provider: String, key: String) {
        keys[provider] = key
    }

    override suspend fun delete(provider: String) {
        keys.remove(provider)
    }
}

/** [validKey] 하나만 맞는 키로 보는 확인기. */
fun acceptOnly(validKey: String) = KeyChecker { _, key -> if (key == validKey) DomainResult.Success(Unit) else DomainResult.Failure(DomainError.InvalidKey) }
