package io.github.nosorae.aiopic.engine.answer.port

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 분석 작업 하나가 받은 응답 원문을 단계마다 DB 에 두는 곳(#205). 분석 작업이 포트를 부를 때 코루틴 문맥에 넣어 준다.
 * 포트 구현은 보내기 전에 [take] 로 저장된 원문을 찾고, 받자마자 해석하기 전에 [save] 한다.
 * 그래서 받은 직후 앱이 죽거나 해석에 실패해도 토큰을 쓴 응답이 남아 다시 해석만 하고 다시 부르지 않는다.
 *
 * 문맥으로 넘기는 이유는 포트 시그니처를 바꾸지 않기 위해서다. 문맥에 없으면(품질 확인 도구 등) 늘 보낸다.
 */
class SavedResponses(
    private val read: suspend (stage: String) -> String?,
    private val write: suspend (stage: String, text: String) -> Unit,
) : AbstractCoroutineContextElement(SavedResponses) {
    private val served = mutableSetOf<String>()

    /**
     * 저장된 원문. 한 번 실행에서 단계마다 처음 한 번만 준다. 출력 검사(F5.4)가 같은 단계를 다시 요청하면
     * 같은 원문을 다시 주지 않고 새로 보내게 한다.
     */
    suspend fun take(stage: String): String? = if (served.add(stage)) read(stage) else null

    suspend fun save(stage: String, text: String) = write(stage, text)

    companion object Key : CoroutineContext.Key<SavedResponses>
}
