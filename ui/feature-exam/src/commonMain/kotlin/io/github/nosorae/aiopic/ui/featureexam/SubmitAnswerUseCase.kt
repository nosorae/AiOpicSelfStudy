package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.libs.recorder.api.Recorder

/**
 * 녹음을 멈추고 다음 단계로 간다. [scored] 문항이면 녹음 조각으로 답변을 저장하고 답한 순서에 붙인다.
 * 점검의 연습 문항은 채점하지 않아서 저장하지 않는다.
 * 녹음 도중 조각을 쓰지 못했으면 빈 곳이 있는 답변이라 저장하지 않고 같은 단계에 남는다. 점검도 같다. 장비가 녹음을 못 하는 걸 여기서 알린다.
 */
@Inject
class SubmitAnswerUseCase(
    private val repository: ExamRepository,
    private val recorder: Recorder,
    private val applyExamEventUseCase: ApplyExamEventUseCase,
) {
    suspend operator fun invoke(session: ExamSession, scored: Boolean): SubmitResult {
        val (chunks, failure) = recorder.stop()
        failure?.let { return SubmitResult.Failed(it.toExamError()) }
        val question = repository.question(session)
        if (!scored || question == null) return SubmitResult.Moved(applyExamEventUseCase(session, ExamEvent.Next()))
        // 단계마다 id 가 하나라서 저장 뒤 앱이 죽어 다시 Next 를 눌러도 답변이 둘로 늘지 않는다.
        val answer = Answer(id = "${session.id}-${session.stepIndex}", question = question, examSessionId = session.id, chunkPaths = chunks)
        repository.save(answer)
        return SubmitResult.Moved(applyExamEventUseCase(session, ExamEvent.Next(answer.id)))
    }
}

sealed interface SubmitResult {
    /** 다음 단계로 간 회차. */
    data class Moved(val session: ExamSession) : SubmitResult

    data class Failed(val error: ExamError) : SubmitResult
}
