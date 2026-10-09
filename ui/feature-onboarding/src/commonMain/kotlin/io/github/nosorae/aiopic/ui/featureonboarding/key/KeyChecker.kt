package io.github.nosorae.aiopic.ui.featureonboarding.key

import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult

/**
 * 제공사에 짧은 요청을 보내 키가 맞는지 확인한다(F1.1). 실제 요청은 앱 층이 제공사 클라이언트로 만들어 넣고,
 * 테스트는 람다로 결과를 정한다. 거절은 [io.github.nosorae.aiopic.ui.featureonboarding.DomainError.InvalidKey],
 * 오프라인, 한도 초과, 서버 오류는 [io.github.nosorae.aiopic.ui.featureonboarding.DomainError.Unavailable] 로 돌려준다.
 */
fun interface KeyChecker {
    suspend fun check(provider: KeyProvider, key: String): DomainResult<Unit>
}
