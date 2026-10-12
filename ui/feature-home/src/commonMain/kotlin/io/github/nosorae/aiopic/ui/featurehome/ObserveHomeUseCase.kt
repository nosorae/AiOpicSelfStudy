package io.github.nosorae.aiopic.ui.featurehome

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 다시 보기는 끝까지 본 회차(시간 부족 포함)만 고른다. 미완료로 끝낸 회차는 기록 탭에만 있다.
 * 전체 문항 수는 시험 화면처럼 고른 수준에서 묻는 문항만 센다. 수준을 고르기 전이면 수준 1 의 문항 수다.
 */
@Inject
class ObserveHomeUseCase(private val examSessionSource: ExamSessionSource, private val spec: ExamSpec) {
    private val overview = ExamOverview(
        minTopics = spec.steps.filterIsInstance<ExamStep.Survey>().firstOrNull()?.minTopics ?: 0,
        levelCount = spec.levelCount,
        minQuestions = askCount(level = 1),
        maxQuestions = askCount(level = spec.levelCount),
        minutes = spec.timeLimit.inWholeMinutes.toInt(),
        recalibrateAfter = spec.steps.indexOf(ExamStep.Recalibrate).takeIf { it >= 0 }?.let { at -> spec.steps.take(at).count { it is ExamStep.Ask } },
    )

    private fun askCount(level: Int) = spec.steps.count { it is ExamStep.Ask && it.minLevel <= level }

    operator fun invoke(): Flow<HomeUiState> = combine(examSessionSource.sessions(), examSessionSource.answers()) { sessions, answers ->
        val bySession = answers.groupBy { it.examSessionId }
        val resume = sessions.lastOrNull { it.status == ExamStatus.InProgress }
        HomeUiState(
            resumeSessionId = resume?.id,
            resumeDone = resume?.answerIds?.size ?: 0,
            resumeTotal = resume?.let { askCount(it.level ?: 1) } ?: 0,
            pastSessions = sessions
                .mapIndexed { index, session ->
                    val own = bySession[session.id].orEmpty()
                    PastSession(
                        id = session.id,
                        number = index + 1,
                        gradeRange = session.grade?.let { if (it.low == it.high) it.low else "${it.low}~${it.high}" },
                        timeUp = session.status == ExamStatus.TimeUp,
                        gradeFailed = session.gradeError != null,
                        // 기록 탭(ObserveHistoryUseCase)과 같은 셈이라 같은 회차가 두 탭에서 같은 진행으로 보인다.
                        answered = own.size,
                        analyzed = own.count { it.analysis is AnswerAnalysis.Done || it.analysis is AnswerAnalysis.Unscorable },
                    ).takeIf { session.status == ExamStatus.Finished || session.status == ExamStatus.TimeUp }
                }
                .filterNotNull()
                .asReversed(),
            overview = overview,
        )
    }
}
