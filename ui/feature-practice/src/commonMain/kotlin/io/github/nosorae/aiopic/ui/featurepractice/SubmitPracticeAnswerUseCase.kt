package io.github.nosorae.aiopic.ui.featurepractice

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.libs.recorder.api.Recorder

/**
 * 녹음을 멈추고 녹음 조각으로 답변을 저장한다. 모의고사 회차가 없는 답변이라 `examSessionId` 는 null 이다.
 * 녹음 도중 조각을 쓰지 못했으면 빈 곳이 있는 답변이라 저장하지 않고 그 이유를 돌려준다. 저장했으면 null 이다.
 */
@Inject
class SubmitPracticeAnswerUseCase(
    private val repository: PracticeRepository,
    private val recorder: Recorder,
) {
    suspend operator fun invoke(question: Question, answerId: String): PracticeError? {
        val (chunks, failure) = recorder.stop()
        failure?.let { return it.toPracticeError() }
        repository.save(Answer(id = answerId, question = question, examSessionId = null, chunkPaths = chunks))
        return null
    }
}
