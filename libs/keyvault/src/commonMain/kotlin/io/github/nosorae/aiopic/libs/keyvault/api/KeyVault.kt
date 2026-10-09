package io.github.nosorae.aiopic.libs.keyvault.api

/**
 * 사용자가 넣은 API 키를 기기 보안 저장소(Android Keystore, iOS Keychain)에 둔다(F1.2).
 * 키는 제공사 이름으로 구분해 따로 저장한다. 한 제공사 키를 지워도 다른 제공사 키는 남는다.
 * 제공사 이름은 이 라이브러리가 알지 못하는 문자열이다.
 */
public interface KeyVault {
    /** 저장된 키가 없으면 null 이다. */
    public suspend fun get(provider: String): String?

    /** 같은 제공사 키가 있으면 바꾼다. */
    public suspend fun put(provider: String, key: String)

    public suspend fun delete(provider: String)
}
