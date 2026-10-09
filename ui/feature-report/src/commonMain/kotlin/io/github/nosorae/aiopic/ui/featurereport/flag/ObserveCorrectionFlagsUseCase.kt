package io.github.nosorae.aiopic.ui.featurereport.flag

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 답변 하나에서 "틀림"을 누른 교정의 순서. */
@Inject
class ObserveCorrectionFlagsUseCase(private val store: FlagStore) {
    operator fun invoke(answerId: String): Flow<Set<Int>> = store.flags().map { flags -> flags.filter { it.answerId == answerId }.map { it.index }.toSet() }
}
