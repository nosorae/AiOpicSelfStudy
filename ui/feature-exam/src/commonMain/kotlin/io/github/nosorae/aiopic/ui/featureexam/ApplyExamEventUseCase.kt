package io.github.nosorae.aiopic.ui.featureexam

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.session.advance
import io.github.nosorae.aiopic.engine.session.model.ExamEvent
import io.github.nosorae.aiopic.engine.session.model.ExamSession

/** 일어난 일로 다음 회차를 계산하고, 바뀌었으면 통째로 저장한다(F3.1). */
@Inject
class ApplyExamEventUseCase(
    private val repository: ExamRepository,
    private val content: ExamContent,
) {
    suspend operator fun invoke(session: ExamSession, event: ExamEvent): ExamSession = advance(content.spec, session, event).also { if (it != session) repository.save(it) }
}
