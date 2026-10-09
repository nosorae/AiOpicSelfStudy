package io.github.nosorae.aiopic.ui.featureonboarding

/** 예상되는 실패. sealed 라서 화면이 경우마다 문구를 고르고, 경우가 늘면 컴파일러가 빠진 문구를 알려 준다. */
// ponytail: DomainResult 와 함께 공용 모듈로 옮긴다. sealed 하위 타입은 같은 모듈에 있어야 하므로 경우도 같이 옮긴다.
sealed interface DomainError {
    /** 제공사가 키를 거절했다. */
    data object InvalidKey : DomainError

    /** 오프라인, 한도 초과, 서버 오류라 맞는지 모른다. 다시 시도하면 될 수 있다. */
    data object Unavailable : DomainError
}
