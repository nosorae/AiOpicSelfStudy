package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * [key] 의 축하를 처음 하는 것이면 본 것으로 남기고 true 를 돌려준다(#198). 이미 했으면 false 다.
 * 표시는 저장소에 있어서 화면을 다시 열거나 앱을 다시 켜도 같은 축하가 다시 터지지 않는다.
 */
@Inject
class CelebrateOnceUseCase(private val store: ReportStore) {
    suspend operator fun invoke(key: String): Boolean {
        if (key in store.seen().first()) return false
        store.markSeen(key, 1)
        return true
    }
}
