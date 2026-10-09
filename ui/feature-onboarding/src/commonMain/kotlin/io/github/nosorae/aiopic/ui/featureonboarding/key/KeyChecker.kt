package io.github.nosorae.aiopic.ui.featureonboarding.key

/**
 * 제공사에 짧은 요청을 보내 키가 맞는지 확인한다(F1.1). 실제 요청은 앱 층이 제공사 클라이언트로 만들어 넣고,
 * 테스트는 람다로 결과를 정한다.
 */
fun interface KeyChecker {
    suspend fun check(provider: KeyProvider, key: String): KeyCheckResult
}

enum class KeyCheckResult {
    Valid,

    /** 제공사가 키를 거절했다. 저장하지 않는다. */
    Invalid,

    /** 오프라인, 한도 초과, 서버 오류라 맞는지 모른다. 저장하지 않고 다시 시도하게 한다. */
    Retryable,
}
