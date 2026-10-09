package io.github.nosorae.aiopic.ui.featuresettings.export

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 내보낼 표시 수와 JSON 본문. */
data class FlagExport(val count: Int, val json: String)

/**
 * "틀림"을 누른 교정을 JSON 배열 하나로 묶는다(F8.3). 항목마다 교정을 답변 id(`answerId`)와 `Feedback.corrections`
 * 안의 순서(`index`)로 가리키고, 그 교정을 판단한 재료인 전문, 원문과 고친 문장, 분석한 제공사와 모델, 프롬프트 버전을 함께 넣는다.
 * 프롬프트를 고칠 때 어느 버전이 무엇을 잘못 고쳤는지 이 파일만 보고 알 수 있어야 해서다(F8.4).
 *
 * 답변 저장 순서, 교정 순서로 줄 세운다. 가리키는 교정이 없는 표시(지운 답변, 범위 밖 순서, 분석 전 답변)는 뺀다.
 */
@Inject
class ObserveFlagExportUseCase(private val store: FlagExportStore) {
    operator fun invoke(): Flow<FlagExport> = combine(store.answers(), store.flags()) { answers, flags ->
        val items = answers.flatMap { answer ->
            val done = answer.analysis as? AnswerAnalysis.Done ?: return@flatMap emptyList()
            val feedback = done.feedback
            flags.filter { it.answerId == answer.id && it.index in feedback.corrections.indices }.map { it.index }.sorted().map { index ->
                buildJsonObject {
                    put("answerId", answer.id)
                    put("index", index)
                    put("transcript", feedback.transcript)
                    put("original", feedback.corrections[index].original)
                    put("corrected", feedback.corrections[index].corrected)
                    put("provider", done.analyzedBy.provider)
                    put("model", done.analyzedBy.modelId)
                    put("promptVersion", done.analyzedBy.promptVersion)
                }
            }
        }
        FlagExport(items.size, pretty.encodeToString(JsonArray.serializer(), JsonArray(items)))
    }

    private companion object {
        /** 이슈 본문에 붙여 사람이 읽으므로 줄을 나눈다. */
        val pretty = Json { prettyPrint = true }
    }
}
