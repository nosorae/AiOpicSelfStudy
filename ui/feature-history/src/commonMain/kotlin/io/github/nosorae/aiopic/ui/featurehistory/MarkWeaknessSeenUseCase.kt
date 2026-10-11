package io.github.nosorae.aiopic.ui.featurehistory

import dev.zacsweers.metro.Inject

/** 약점 줄의 지금 점수를 본 것으로 남긴다(#198). 다음에 기록을 열면 이 점수보다 올라야 다시 축하한다. */
@Inject
class MarkWeaknessSeenUseCase(private val store: HistoryStore) {
    suspend operator fun invoke(weakness: HistoryWeakness) = store.markSeen(weakness.type.seenKey, weakness.score)
}
