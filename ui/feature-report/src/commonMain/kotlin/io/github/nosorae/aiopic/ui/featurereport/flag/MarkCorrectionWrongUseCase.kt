package io.github.nosorae.aiopic.ui.featurereport.flag

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag

/** 교정 하나에 "틀림"을 붙이거나 뗀다(F8.1). 잘못 누른 표시를 되돌릴 수 있어야 약점 점수가 그대로 돌아온다. */
@Inject
class MarkCorrectionWrongUseCase(private val store: FlagStore) {
    suspend operator fun invoke(answerId: String, index: Int, wrong: Boolean) = store.mark(CorrectionFlag(answerId, index), wrong)
}
