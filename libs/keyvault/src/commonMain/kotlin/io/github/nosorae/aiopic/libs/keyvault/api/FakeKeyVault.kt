package io.github.nosorae.aiopic.libs.keyvault.api

/** 테스트용. 키를 메모리에만 둔다. 화면과 엔진 테스트가 함께 쓰려고 commonMain 에 둔다. */
public class FakeKeyVault : KeyVault {
    private val keys = mutableMapOf<String, String>()

    override suspend fun get(provider: String): String? = keys[provider]

    override suspend fun put(provider: String, key: String) {
        keys[provider] = key
    }

    override suspend fun delete(provider: String) {
        keys.remove(provider)
    }
}
