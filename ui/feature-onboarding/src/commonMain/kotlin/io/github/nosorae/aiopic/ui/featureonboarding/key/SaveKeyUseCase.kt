package io.github.nosorae.aiopic.ui.featureonboarding.key

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.libs.keyvault.api.KeyVault
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult

/** 키를 [cleanKey] 로 정리해 확인하고 맞을 때만 제공사 이름으로 저장한다(F1.1, F1.2). 틀린 키는 남기지 않는다. */
@Inject
class SaveKeyUseCase(
    private val keyVault: KeyVault,
    private val keyChecker: KeyChecker,
) {
    suspend operator fun invoke(provider: KeyProvider, key: String): DomainResult<Unit> {
        val cleaned = cleanKey(key)
        val result = keyChecker.check(provider, cleaned)
        if (result is DomainResult.Success) keyVault.put(provider.id, cleaned)
        return result
    }
}

/**
 * 복사하면서 키에 딸려 오는 글자를 뺀다. 마크다운 이스케이프 역슬래시(`AQ\.` 처럼), 공백과 줄바꿈,
 * 눈에 안 보이는 문자(제로폭 공백 U+200B~U+200D, U+2060, U+FEFF)다. 키 글자(영문, 숫자, `.`, `-`, `_`)는 그대로 둔다.
 */
fun cleanKey(key: String): String = key.replace(Regex("""[\\\s\u200B-\u200D\u2060\uFEFF]"""), "")
