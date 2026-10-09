package io.github.nosorae.aiopic.libs.keyvault.api

/** 테스트용. 키를 메모리에만 둔다. 다른 모듈 테스트가 쓰게 되면 공용 테스트 모듈로 옮긴다. */
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
