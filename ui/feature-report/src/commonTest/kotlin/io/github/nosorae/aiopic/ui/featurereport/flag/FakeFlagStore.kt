package io.github.nosorae.aiopic.ui.featurereport.flag

import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeFlagStore : FlagStore {
    val flags = MutableStateFlow(emptySet<CorrectionFlag>())

    override fun flags(): Flow<Set<CorrectionFlag>> = flags

    override suspend fun mark(flag: CorrectionFlag, wrong: Boolean) = flags.update { if (wrong) it + flag else it - flag }
}
