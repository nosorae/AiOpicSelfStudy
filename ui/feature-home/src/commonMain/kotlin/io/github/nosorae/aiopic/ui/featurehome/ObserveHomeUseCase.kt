package io.github.nosorae.aiopic.ui.featurehome

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class ObserveHomeUseCase(private val examSessionSource: ExamSessionSource) {
    operator fun invoke(): Flow<HomeUiState> = examSessionSource.sessions().map { sessions ->
        HomeUiState(
            resumeSessionId = sessions.lastOrNull { it.status == ExamStatus.InProgress }?.id,
            pastSessions = sessions
                .mapIndexed { index, session ->
                    PastSession(
                        id = session.id,
                        number = index + 1,
                        gradeRange = session.grade?.let { if (it.low == it.high) it.low else "${it.low}~${it.high}" },
                        timeUp = session.status == ExamStatus.TimeUp,
                    ).takeIf { session.status != ExamStatus.InProgress }
                }
                .filterNotNull()
                .asReversed(),
        )
    }
}
