package io.github.nosorae.aiopic.ui.featurehome

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 전체 문항 수는 시험 화면처럼 고른 수준에서 묻는 문항만 센다. 수준을 고르기 전이면 수준 1 의 문항 수다. */
@Inject
class ObserveHomeUseCase(private val examSessionSource: ExamSessionSource, private val spec: ExamSpec) {
    operator fun invoke(): Flow<HomeUiState> = examSessionSource.sessions().map { sessions ->
        val resume = sessions.lastOrNull { it.status == ExamStatus.InProgress }
        HomeUiState(
            resumeSessionId = resume?.id,
            resumeDone = resume?.answerIds?.size ?: 0,
            resumeTotal = resume?.let { session -> spec.steps.count { it is ExamStep.Ask && it.minLevel <= (session.level ?: 1) } } ?: 0,
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
