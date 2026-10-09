package io.github.nosorae.aiopic.ui.featureonboarding

/**
 * UseCase 의 결과. 예상되는 실패는 예외로 넘기지 않고 [Failure] 에 [DomainError] 를 담아 돌려준다.
 * 성공과 실패가 타입으로 갈려서 화면 상태의 오류 필드에 성공이 들어갈 수 없다.
 */
// ponytail: 아직 이 화면 모듈 하나만 쓴다. 두 번째 모듈이 쓰게 되면 공용 모듈로 옮긴다.
sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>

    data class Failure(val error: DomainError) : DomainResult<Nothing>
}
