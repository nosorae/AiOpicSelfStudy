package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.answer.weakness.CorrectionFlag
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.ui.featurepractice.PracticeRepository
import kotlinx.coroutines.flow.first

/**
 * 한 문항 연습의 저장소. 답변은 시험과 같은 [OpicStore] 에 두고 저장하자마자 분석 대기열에 넣는다(F5.1).
 * 녹음은 [recordingsRoot] 아래 `practice/답변 id` 폴더에 써서 회차 폴더와 섞이지 않는다.
 */
class StorePracticeRepository(
    private val store: OpicStore,
    private val queue: WorkQueue,
    private val pool: suspend () -> List<Question>,
    private val recordingsRoot: String,
) : PracticeRepository {
    override suspend fun pool(): List<Question> = pool.invoke()

    override suspend fun answers(): List<Answer> = store.answers().first()

    override suspend fun flaggedCorrections(): Set<CorrectionFlag> = store.flags().first()

    override fun recordingDirectory(answerId: String): String = "$recordingsRoot/practice/$answerId"

    override suspend fun save(answer: Answer) {
        store.save(answer)
        queue.enqueue(answer.id, answer.id)
    }
}
